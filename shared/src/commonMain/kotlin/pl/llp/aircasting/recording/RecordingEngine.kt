package pl.llp.aircasting.recording

import co.touchlab.kermit.Logger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.bluetooth.SensorReading
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementRepository
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** What the user typed on the record screens. */
data class NewSessionDetails(val name: String, val tags: List<String>)

/**
 * Turns an [AirBeamLink] into a stored mobile session. Talks to the link only; never uploads —
 * the upload loop picks sessions up from the database.
 */
@OptIn(ExperimentalUuidApi::class)
class RecordingEngine(
  private val link: AirBeamLink,
  private val sessions: SessionRepository,
  private val streams: MeasurementStreamRepository,
  private val measurements: MeasurementRepository,
  private val location: PhoneLocation,
  private val host: BackgroundSessionHost,
  private val clock: Clock,
  private val timeZone: TimeZone,
) {
  private val log = Logger.withTag("RecordingEngine")

  private class Active(val uuid: String, val streamIds: Map<StreamSpec, Long>)

  /** The session recording over the link right now; null when there is none. */
  private val active = MutableStateFlow<Active?>(null)
  /**
   * Every write for the active session goes through here, and checks [active] inside it. So once
   * stop / standalone has cleared it, a save or a status change that was already on its way
   * finds nothing to write to — it can't reopen a finished session or land after `end_time`.
   */
  private val writes = Mutex()


  /** Never returns; cancel the calling scope to stop. Launched once for the life of the process. */
  suspend fun run(): Unit = coroutineScope {
    launch { saveReadings() }
    launch { trackLocation() }
    followLink()
  }

  /** Whether a session is recording over the link right now. */
  val recording: Flow<Boolean> = active.map { it != null }.distinctUntilChanged()

  private suspend fun trackLocation() {
    recording.collectLatest { on -> if (on) location.track() }
  }

  /** Configures the device first and writes nothing unless it answers [ConfigResult.Success]. */
  suspend fun start(target: DiscoveredAirBeam, details: NewSessionDetails): ConfigResult {
    val specs = streamsFor(target.device)
    if (specs.isEmpty()) {
      log.w { "No streams catalogued for ${target.device}; not starting" }
      return ConfigResult.BadConfigFailure
    }

    val uuid = Uuid.random()
    val result = link.configure(SessionConfig.Mobile(uuid))
    if (result != ConfigResult.Success) return result

    val session = Session(
      uuid = uuid.toString(),
      type = SessionType.MOBILE,
      status = SessionStatus.RECORDING,
      name = details.name,
      tags = details.tags,
      startTime = clock.now(),
      timeZone = timeZone.id,
      measurementInterval = 1.seconds, // what MiniV2.mobileSessionConfigCommand sends
      deviceId = target.id.value,
      deviceModel = target.device.model,
    )
    sessions.insert(session)
    val streamIds = specs.associateWith { streams.findOrCreate(session.uuid, it.sensorName, it.unitSymbol) }
    active.value = Active(session.uuid, streamIds)
    host.start()
    log.i { "Recording ${session.uuid} on ${target.name}" }
    return result
  }

  /** Ends the session; the upload loop drains it and calls `/finish`. */
  suspend fun stop() = writes.withLock {
    val session = active.getAndUpdate { null } ?: return@withLock
    link.disconnect()
    sessions.finish(session.uuid, clock.now())
    host.stop()
  }
  /** Drops the link but not the session: the AirBeam keeps recording on its own. */
  suspend fun goStandalone() = writes.withLock {
    val session = active.getAndUpdate { null } ?: return@withLock
    link.disconnect()
    sessions.updateStatus(session.uuid, SessionStatus.DISCONNECTED)
    host.stop()
  }

  private suspend fun saveReadings() {
    link.readings.collect { reading -> writes.withLock { save(reading) } }
  }

  private suspend fun save(reading: SensorReading) {
    val session = active.value ?: return
    val streamId = session.streamIds[StreamSpec(reading.sensorName, reading.unitSymbol)]
    if (streamId == null) {
      log.w { "${session.uuid}: no stream for ${reading.sensorName} [${reading.unitSymbol}]; dropped" }
      return
    }
    measurements.insertRecorded(
      listOf(
        Measurement(
          streamId = streamId,
          time = reading.measuredAt ?: clock.now(),
          value = reading.value,
          location = location.latest() ?: DEFAULT_LOCATION,
        )
      )
    )
  }

  /** The link reconnects on its own; the session only mirrors whether data can flow. */
  private suspend fun followLink() {
    link.state.collect { state ->
      val status = when (state) {
        is LinkState.Connected -> SessionStatus.RECORDING
        is LinkState.Reconnecting, is LinkState.Failed -> SessionStatus.DISCONNECTED
        LinkState.Idle, is LinkState.Connecting -> return@collect
      }
      writes.withLock { active.value?.let { sessions.updateStatus(it.uuid, status) } }
    }
  }
}

/** Legacy `Session.Location.DEFAULT_LOCATION`: before the phone's first fix of the session. */
private val DEFAULT_LOCATION = GeoLocation(40.7128, -74.0060)

/** v3 `device.model`. Legacy sends none for mobile; these follow its CSV `deviceName`s. */
private val AirBeamDevice.model: String
  get() = when (this) {
    AirBeamDevice.Mini.V2 -> "AirBeamMini"
    AirBeamDevice.AirBeam3 -> "AirBeam3"
    AirBeamDevice.AirBeam2, AirBeamDevice.Mini.V1 ->
      error("$this is not catalogued; start() stops before asking for its model")
  }
package pl.llp.aircasting.recording

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.SessionConfig
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
  private val host: BackgroundSessionHost,
  private val clock: Clock,
  private val timeZone: TimeZone,
) {
  private val log = Logger.withTag("RecordingEngine")

  /** Uuid of the session recording over the link right now; null when there is none. */
  private val active = MutableStateFlow<String?>(null)

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
    specs.forEach { streams.findOrCreate(session.uuid, it.sensorName, it.unitSymbol) }
    active.value = session.uuid
    host.start()
    log.i { "Recording ${session.uuid} on ${target.name}" }
    return result
  }

  /** Ends the session; the upload loop drains it and calls `/finish`. */
  suspend fun stop() {
    val uuid = active.getAndUpdate { null } ?: return
    link.disconnect()
    sessions.finish(uuid, clock.now())
    host.stop()
  }

  /** Drops the link but not the session: the AirBeam keeps recording on its own. */
  suspend fun goStandalone() {
    val uuid = active.getAndUpdate { null } ?: return
    link.disconnect()
    sessions.updateStatus(uuid, SessionStatus.DISCONNECTED)
    host.stop()
  }
}

/** v3 `device.model`. Legacy `SessionParams` sends "AirBeamMini"; others come with their catalog entries. */
private val AirBeamDevice.model: String
  get() = when (this) {
    AirBeamDevice.Mini.V2 -> "AirBeamMini"
    AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1 ->
      error("$this is not catalogued; start() stops before asking for its model")
  }
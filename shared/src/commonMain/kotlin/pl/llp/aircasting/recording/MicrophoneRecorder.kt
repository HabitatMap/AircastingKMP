package pl.llp.aircasting.recording

import co.touchlab.kermit.Logger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.MeasurementRepository
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.settings.mic.MicrophoneLevelSource
import pl.llp.aircasting.settings.mic.calibrate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Legacy `MicrophoneDeviceItem.DEFAULT_ID`: how a mic session is told apart from an AirBeam one. */
const val PhoneMicDeviceId = "Builtin"

/** v3 `device.model` is required; legacy sent none, so this is ours. */
private const val PhoneMicModel = "Phone Microphone"

/** Legacy `MicrophoneReader`; a sensor v3 knows, so no custom-sensor fields. */
private val PhoneMicStream = StreamSpec("Phone Microphone", "dB")

/** Records the phone's microphone as a mobile session. The upload loop takes it from the DB like any other. */
@OptIn(ExperimentalUuidApi::class)
class MicrophoneRecorder(
  private val levels: MicrophoneLevelSource,
  private val calibrationOffset: () -> Int,
  private val sessions: SessionRepository,
  private val streams: MeasurementStreamRepository,
  private val measurements: MeasurementRepository,
  private val location: PhoneLocation,
  private val host: BackgroundSessionHost,
  private val clock: Clock,
  private val timeZone: TimeZone,
) {
  private val log = Logger.withTag("MicrophoneRecorder")

  private class Active(val uuid: String, val streamId: Long)

  private val active = MutableStateFlow<Active?>(null)

  /** Same role as in [RecordingEngine]: once finish clears [active], no write can land after `end_time`. */
  private val writes = Mutex()

  /** Never returns; launched once for the life of the process. The mic is open only while a session records. */
  suspend fun run() {
    active.collectLatest { session -> if (session != null) record(session) }
  }

  /** @return false while a mic session already records: one microphone, one session (legacy). */
  suspend fun start(details: NewSessionDetails): Boolean = writes.withLock {
    if (active.value != null) return@withLock false
    val session = Session(
      uuid = Uuid.random()
        .toString(),
      type = SessionType.MOBILE,
      status = SessionStatus.RECORDING,
      name = details.name,
      tags = details.tags,
      startTime = clock.now(),
      timeZone = timeZone.id,
      measurementInterval = 1.seconds,
      deviceId = PhoneMicDeviceId,
      deviceModel = PhoneMicModel,
    )
    sessions.insert(session)
    val streamId =
      streams.findOrCreate(session.uuid, PhoneMicStream.sensorName, PhoneMicStream.unitSymbol)
    active.value = Active(session.uuid, streamId)
    host.start()
    log.i { "Recording ${session.uuid} on the phone microphone" }
    true
  }

  /** @return false when [uuid] isn't the mic session, so the caller hands it on. */
  suspend fun finish(uuid: String): Boolean = writes.withLock {
    if (active.value?.uuid != uuid) return@withLock false
    active.value = null
    host.stop()
    sessions.finish(uuid, clock.now())
    true
  }

  private suspend fun record(session: Active) = coroutineScope {
    launch { location.track() }
    var lastSecond: Long? = null
    levels.levels()
      .collect { dbFs ->
        val now = clock.now()
        // Legacy MicrophoneReader.isSecondTheSame: the first block of each new second is the reading.
        if (now.epochSeconds == lastSecond) return@collect
        lastSecond = now.epochSeconds
        writes.withLock {
          if (active.value !== session) return@withLock
          measurements.insertRecorded(
            listOf(
              Measurement(
                streamId = session.streamId,
                time = now,
                value = calibrate(dbFs, calibrationOffset()),
                location = location.latest() ?: DEFAULT_LOCATION,
              )
            )
          )
        }
      }

  }
}
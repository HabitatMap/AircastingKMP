package pl.llp.aircasting.recording
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.CreateFixedSessionRequestDto
import pl.llp.aircasting.data.network.CreateStreamPayloadDto
import pl.llp.aircasting.data.network.DeviceDto
import pl.llp.aircasting.data.network.FixedSessionsApi
import pl.llp.aircasting.domain.GeoLocation
import kotlin.uuid.Uuid

/** Where the AirBeam hangs. Indoor sessions don't show a location on the public map. */
sealed interface Placement {
  data object Indoor : Placement
  data class Outdoor(val location: GeoLocation) : Placement
}

data class NewFixedSession(
  val uuid: Uuid,
  val name: String,
  val tags: List<String>,
  val device: DiscoveredAirBeam,
  val placement: Placement,
)

/**
 * What the AirBeam needs to upload on its own. Mini V2: [sessionToken] (32 hex chars) and sensor_type_ids
 * by sensor_name. V1 devices upload to the realtime API as the user, so they get [userToken] instead.
 */
data class RegisteredFixedSession(val sessionToken: String, val sensorTypeIds: Map<String, Int>, val userToken: String)

/**
 * Backend half of a fixed start (DOMAIN_KNOWLEDGE.md "Starting a fixed session"): creates the
 * session on v3 before the AirBeam is configured, as legacy `RecordingHandler` does, then sets tags.
 */
class FixedSessionRegistrar(
  private val api: FixedSessionsApi,
  private val auth: AuthSession,
  private val timeZone: TimeZone,
) {
  private val log = Logger.withTag("FixedSessionRegistrar")

  /** Null when signed out; throws when the create fails. A failed tag update is only logged. */
  suspend fun register(session: NewFixedSession): RegisteredFixedSession? {
    val token = auth.state.value.tokenOrNull ?: return null
    val created = api.createSession(token, session.toCreateRequest(timeZone.id))
    if (session.tags.isNotEmpty()) setTags(token, session)
    return RegisteredFixedSession(
      sessionToken = created.sessionToken,
      sensorTypeIds = created.streams.associate { it.sensorName to it.sensorTypeId },
      userToken = token,
    )
  }

  /** The session already exists by now: failing the start over tags would strand it unconfigured. */
  private suspend fun setTags(token: String, session: NewFixedSession) {
    try {
      api.updateTags(token, session.uuid.toString(), session.tags.joinToString(" "))
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (e: Exception) {
      log.w(e) { "${session.uuid}: tags not saved" }
    }
  }
}

/** The backend's placeholder for indoor sessions (legacy `Session.FAKE_LOCATION`). */
private val IndoorPlaceholder = GeoLocation(200.0, 200.0)

/** The coordinates a fixed session is registered and configured with. */
internal val Placement.coordinates: GeoLocation
  get() = (this as? Placement.Outdoor)?.location ?: IndoorPlaceholder

private fun NewFixedSession.toCreateRequest(timeZone: String): CreateFixedSessionRequestDto {
  val location = placement.coordinates
  return CreateFixedSessionRequestDto(
    uuid = uuid.toString(),
    title = name,
    latitude = location.latitude,
    longitude = location.longitude,
    isContributingToCrowdMap = true, // legacy SessionBuilder: always true for fixed
    isIndoor = placement == Placement.Indoor,
    timeZone = timeZone,
    device = DeviceDto(macAddress = device.id.value, model = device.device.model),
    streams = streamsFor(device.device).map { CreateStreamPayloadDto(it.sensorName, it.unitSymbol) },
  )
}
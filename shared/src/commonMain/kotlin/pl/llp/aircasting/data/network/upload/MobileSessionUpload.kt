package pl.llp.aircasting.data.network.upload

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.CreateMobileSessionRequestDto
import pl.llp.aircasting.data.network.CreateStreamPayloadDto
import pl.llp.aircasting.data.network.DeviceDto
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.domain.session.MeasurementStream
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionRepository

/**
 * Brings one recorded mobile session up to date on the server: create (once), upload the
 * outbox, then `/finish` (once, after the user stopped). Each step is skipped when already
 * done, so calling it again after a failure resumes where it stopped.
 */
class MobileSessionUpload(
  private val api: MobileSessionsApi,
  private val sessions: SessionRepository,
  private val streams: MeasurementStreamRepository,
  private val uploader: MobileMeasurementUploader,
  private val auth: AuthSession,
) : SessionUploader {
  private val log = Logger.withTag("SessionUpload")

  /** Throws on the first failed request; whatever was done before it stays done. */
  override suspend fun upload(sessionUuid: String) {
    val token = auth.state.value.tokenOrNull ?: return
    val session = sessions.find(sessionUuid) ?: return
    val local = streams.findBySession(sessionUuid)
    if (local.any { it.sensorTypeId == null }) {
      val assigned = createOrReadBack(token, session, local)
      local.forEach { stream ->
        assigned[stream.sensorName]?.let { streams.setSensorTypeId(stream.id, it) }
          ?: log.e { "$sessionUuid: server assigned no sensor_type_id to ${stream.sensorName}" }
      }
    }

    uploader.upload(sessionUuid)

    val endTime = session.endTime
    if (endTime != null && !session.isFinishedOnServer) {
      api.finishSession(token, sessionUuid, endTime)
      sessions.markFinishedOnServer(sessionUuid)
    }
  }
  /** @return sensor_type_id by sensor_name. */
  private suspend fun createOrReadBack(
    token: String,
    session: Session,
    local: List<MeasurementStream>,
  ): Map<String, Int> = try {
    api.createSession(token, session.toCreateRequest(local))
      .streams.associate { it.sensorName to it.sensorTypeId }
  } catch (e: ClientRequestException) {
    if (e.response.status != HttpStatusCode.Conflict) throw e
    // session_uuid_taken: an earlier create landed but its response never reached us.
    api.getSession(token, session.uuid)
      .streams.mapNotNull { (name, stream) -> stream.sensorTypeId?.let { name to it } }
      .toMap()
  }
}

private fun Session.toCreateRequest(streams: List<MeasurementStream>) = CreateMobileSessionRequestDto(
  uuid = uuid,
  title = name,
  timeZone = timeZone,
  isContributingToCrowdMap = isContributingToCrowdMap,
  tagList = tags.joinToString(" "),
  device = DeviceDto(
    macAddress = requireNotNull(deviceId) { "$uuid: mobile session without a device id" },
    model = requireNotNull(deviceModel) { "$uuid: mobile session without a device model" },
  ),
  streams = streams.map { CreateStreamPayloadDto(sensorName = it.sensorName, unitSymbol = it.unitSymbol) },
)

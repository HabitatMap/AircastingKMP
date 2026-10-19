package pl.llp.aircasting.data.network.upload

import co.touchlab.kermit.Logger
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.UploadOutbox

/** Drains one mobile session's outbox to `POST /api/v3/mobile_sessions/{uuid}/measurements`. */
class MobileMeasurementUploader(
  private val api: MobileSessionsApi,
  private val outbox: UploadOutbox,
  private val streams: MeasurementStreamRepository,
  private val auth: AuthSession,
) {
  private val log = Logger.withTag("Upload")

  /**
   * Sends what is pending now, in batches of at most [MobileMeasurementPayload.MaxFrames].
   * Throws if a request fails — rows are acknowledged only after a successful response, so
   * anything unsent stays queued for the next call.
   */
  suspend fun upload(sessionUuid: String) {
    val token = auth.state.value.tokenOrNull ?: return
    val sensorTypeIds = streams.findBySession(sessionUuid)
      .mapNotNull { stream -> stream.sensorTypeId?.let { stream.id to it } }
      .toMap()

    do {
      val pending = outbox.pending(sessionUuid, MobileMeasurementPayload.MaxFrames)
      val sendable = pending.mapNotNull { measurement ->
        val sensorTypeId = sensorTypeIds[measurement.streamId] ?: return@mapNotNull null
        val location = measurement.location ?: return@mapNotNull null
        measurement to MobileFrame(measurement.time, sensorTypeId, measurement.value, location)
      }
      if (sendable.size < pending.size) {
        log.e { "$sessionUuid: ${pending.size - sendable.size} rows unsendable (stream without sensor_type_id, or no location)" }
      }
      if (sendable.isEmpty()) return

      api.uploadMeasurementsBinary(token, sessionUuid, MobileMeasurementPayload.encode(sendable.map { it.second }))
      outbox.acknowledge(sendable.map { it.first })
    } while (pending.size == MobileMeasurementPayload.MaxFrames)
  }
}
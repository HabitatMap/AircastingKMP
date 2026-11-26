package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Standard error envelope returned on 4xx/5xx responses across all /api/v3 endpoints.
 */
@Serializable
data class ErrorDto(
  @SerialName("error_code") val errorCode: String,
  val message: String,
  val fields: Map<String, List<String>>? = null,
)
/**
 * Pagination metadata returned in v3 collection queries.
 */
@Serializable
data class PaginationMetaDto(
  val total: Int,
  val page: Int,
  @SerialName("per_page") val perPage: Int,
  @SerialName("total_pages") val totalPages: Int,
)
// --- Mobile Sessions v3 ---
@Serializable
data class MobileSessionListResponseDto(
  val sessions: List<MobileSessionSummaryDto> = emptyList(),
  val meta: PaginationMetaDto,
  @SerialName("share_url") val shareUrl: String? = null,
  val streams: Map<String, SessionStreamDto> = emptyMap(),
)
@Serializable
data class MobileSessionSummaryDto(
  val uuid: String,
  val title: String,
  val type: String = "MobileSession",
  @SerialName("tag_list") val tagList: String? = null,
  @SerialName("contribute") val isContributingToCrowdMap: Boolean = true,
  @SerialName("time_zone") val timeZone: String,
  @SerialName("start_time") val startTime: Long? = null,
  @SerialName("end_time") val endTime: Long? = null,
  val version: Int = 0,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @SerialName("share_url") val shareUrl: String? = null,
  val streams: Map<String, SessionStreamDto> = emptyMap(),
)
/** `POST /api/v3/mobile_sessions` — flat body, no `session` wrapper. */
@Serializable
data class CreateMobileSessionRequestDto(
  val uuid: String,
  val title: String,
  @SerialName("time_zone") val timeZone: String,
  @SerialName("contribute") val isContributingToCrowdMap: Boolean,
  @SerialName("tag_list") val tagList: String,
  val device: DeviceDto,
  val streams: List<CreateStreamPayloadDto>,
)
@Serializable
data class DeviceDto(
  @SerialName("mac_address") val macAddress: String,
  val model: String,
)
@Serializable
data class CreateMobileSessionResponseDto(
  val streams: List<CreatedStreamDto>,
)
@Serializable
data class FinishMobileSessionRequestDto(
  @SerialName("finished_at") val finishedAt: Long,
)
/** One entry of a session's `streams` object (keyed by sensor_name); aggregates ignored. */
@Serializable
data class SessionStreamDto(
  @SerialName("sensor_type_id") val sensorTypeId: Int? = null,
)

@Serializable
data class CreateStreamPayloadDto(
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  @SerialName("measurement_type") val measurementType: String? = null,
  @SerialName("measurement_short_type") val measurementShortType: String? = null,
  @SerialName("unit_name") val unitName: String? = null,
)
@Serializable
data class UpdateMobileSessionRequestDto(
  val title: String? = null,
  @SerialName("tag_list") val tagList: String? = null,
  @SerialName("contribute") val isContributingToCrowdMap: Boolean? = null,
)
@Serializable
data class NoteDto(
  val id: Long,
  val number: Int? = null,
  val text: String,
  val date: String,
  val latitude: Double,
  val longitude: Double,
  @SerialName("photo_location") val photoLocation: String? = null,
)
// --- Fixed Sessions v3 ---
/** `GET /api/v3/fixed_sessions`: the signed-in user's own fixed sessions, newest first. */
@Serializable
data class FixedSessionListResponseDto(
  val sessions: List<FixedSessionDto> = emptyList(),
  val meta: PaginationMetaDto,
)

/**
 * One fixed session, as `GET /api/v3/fixed_sessions` lists it and `GET /fixed_sessions/{uuid}` returns it.
 * Times are epoch ms, true UTC. Unused fields (`contribute`, `share_url`, `version`…) are dropped by
 * `ignoreUnknownKeys`.
 */
@Serializable
data class FixedSessionDto(
  val uuid: String,
  val title: String,
  /** Space-separated as we send it; the spec's example is comma-separated, so the mapper splits on both. */
  @SerialName("tag_list") val tagList: String = "",
  @SerialName("is_indoor") val isIndoor: Boolean = false,
  /** IANA zone at the sensor: every timestamp is shown on its wall clock. */
  @SerialName("time_zone") val timeZone: String,
  @SerialName("start_time") val startTime: Long? = null,
  /** When the AirBeam last reached the server (not the reading's own time). Null until it first reports. */
  @SerialName("last_measurement_at") val lastMeasurementAt: Long? = null,
  /** Set once the owner finished (decommissioned) the session. */
  @SerialName("finished_at") val finishedAt: Long? = null,
  /** Indoor sessions carry the 200, 200 placeholder. */
  val latitude: Double? = null,
  val longitude: Double? = null,
  val device: FixedSessionDeviceDto? = null,
  /** Keyed by sensor_name. */
  val streams: Map<String, FixedSessionStreamDto> = emptyMap(),
)

/** `POST /fixed_sessions/{uuid}/finish`: only these are guaranteed. */
@Serializable
data class FinishedFixedSessionDto(
  val uuid: String,
  @SerialName("finished_at") val finishedAt: Long? = null,
)

@Serializable
data class FixedSessionDeviceDto(val model: String? = null)

@Serializable
data class FixedSessionStreamDto(
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("unit_symbol") val unitSymbol: String,
  /** Null: the stream never reported. */
  @SerialName("last_measurement") val lastMeasurement: FixedLastMeasurementDto? = null,
)

/** Unlike Explore's rounded value, the fixed endpoints send the raw reading (e.g. 12.5). */
@Serializable
data class FixedLastMeasurementDto(val value: Double, val time: Long)
@Serializable
data class FixedSessionPayloadDto(
  val uuid: String,
  val title: String,
  @SerialName("time_zone") val timeZone: String,
  val latitude: Double,
  val longitude: Double,
  @SerialName("is_indoor") val isIndoor: Boolean = false,
  val streams: List<CreateStreamPayloadDto> = emptyList(),
)
/** `POST /api/v3/fixed_sessions` — flat body, no `session` wrapper, and no `tag_list` (PATCH it after). */
@Serializable
data class CreateFixedSessionRequestDto(
  val uuid: String,
  val title: String,
  val latitude: Double,
  val longitude: Double,
  @SerialName("contribute") val isContributingToCrowdMap: Boolean,
  @SerialName("is_indoor") val isIndoor: Boolean,
  @SerialName("time_zone") val timeZone: String,
  val device: DeviceDto,
  val streams: List<CreateStreamPayloadDto>,
)
@Serializable
data class UpdateFixedSessionRequestDto(
  @SerialName("tag_list") val tagList: String,
)
@Serializable
data class CreateFixedSessionResponseDto(
  @SerialName("share_url") val shareUrl: String,
  @SerialName("session_token") val sessionToken: String,
  val streams: List<CreatedStreamDto> = emptyList(),
)
@Serializable
data class CreatedStreamDto(
  @SerialName("sensor_name") val sensorName: String,
  @SerialName("sensor_type_id") val sensorTypeId: Int,
)
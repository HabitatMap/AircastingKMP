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
@Serializable
data class FixedSessionListResponseDto(
  val sessions: List<FixedSessionSummaryDto> = emptyList(),
  val meta: PaginationMetaDto,
)
@Serializable
data class FixedSessionSummaryDto(
  val uuid: String,
  val title: String,
  @SerialName("share_url") val shareUrl: String,
  @SerialName("session_token") val sessionToken: String? = null,
)
@Serializable
data class CreateFixedSessionRequestDto(
  val session: FixedSessionPayloadDto,
)
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
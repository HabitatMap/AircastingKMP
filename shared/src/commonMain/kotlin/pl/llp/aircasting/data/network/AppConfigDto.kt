package pl.llp.aircasting.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Only what we render. `locale`, `default_locale`, `learn_more` are dropped by `ignoreUnknownKeys`. */
@Serializable
data class AppConfigDto(
  val help: List<LinkDto>,
  @SerialName("aircasting_actions") val aircastingActions: List<LinkDto>,
)

@Serializable
data class LinkDto(val title: String, val url: String)
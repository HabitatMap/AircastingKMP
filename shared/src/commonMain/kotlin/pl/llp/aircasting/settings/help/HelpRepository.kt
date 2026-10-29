package pl.llp.aircasting.settings.help

import pl.llp.aircasting.data.network.AppConfigApi
import pl.llp.aircasting.data.network.LinkDto

data class HelpLink(val title: String, val url: String)

/** Both lists in server display order. */
data class HelpLinks(val help: List<HelpLink>, val actions: List<HelpLink>)

interface HelpRepository {
  suspend fun links(locale: String): HelpLinks
}

class NetworkHelpRepository(private val api: AppConfigApi) : HelpRepository {
  override suspend fun links(locale: String): HelpLinks {
    val config = api.appConfig(locale)
    return HelpLinks(config.help.map { it.toLink() }, config.aircastingActions.map { it.toLink() })
  }
}

private fun LinkDto.toLink() = HelpLink(title, url)
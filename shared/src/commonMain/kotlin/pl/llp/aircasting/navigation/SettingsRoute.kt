package pl.llp.aircasting.navigation

import kotlinx.serialization.Serializable
import pl.llp.aircasting.i18n.Strings


@Serializable
sealed interface SettingsRoute {
  @Serializable data object Root : SettingsRoute
  @Serializable data object Account : SettingsRoute
  @Serializable data object AirBeams : SettingsRoute
  @Serializable data object AppSettings : SettingsRoute
  @Serializable data object Help : SettingsRoute
  @Serializable data object CustomDataServer : SettingsRoute

  companion object {
    val sections: List<SettingsRoute> = listOf(Account, AirBeams, AppSettings, Help)
  }
}

fun Strings.title(route: SettingsRoute): String = when (route) {
  SettingsRoute.Root -> settingsTitle
  SettingsRoute.Account -> settingsAccountTitle
  SettingsRoute.AirBeams -> settingsAirBeamsTitle
  SettingsRoute.AppSettings -> settingsAppTitle
  SettingsRoute.Help -> settingsHelpTitle
  SettingsRoute.CustomDataServer -> customServerTitle
}

fun Strings.subtitle(route: SettingsRoute): String? = when (route) {
  SettingsRoute.Root -> null
  SettingsRoute.Account -> settingsAccountSubtitle
  SettingsRoute.AirBeams -> settingsAirBeamsSubtitle
  SettingsRoute.AppSettings -> settingsAppSubtitle
  SettingsRoute.Help -> settingsHelpSubtitle
  SettingsRoute.CustomDataServer -> null
}

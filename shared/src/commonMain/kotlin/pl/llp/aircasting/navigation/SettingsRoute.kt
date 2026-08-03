package pl.llp.aircasting.navigation

import kotlinx.serialization.Serializable
import pl.llp.aircasting.i18n.Strings

/**
 * Every destination in the Settings stack.
 *
 * `@Serializable` is what makes Navigation Compose's *type-safe* API work: the library derives
 * a route pattern from the serializer, so `composable<SettingsRoute.Account>` and
 * `navigate(SettingsRoute.Account)` are checked by the compiler instead of by string matching.
 * That is why the kotlinx-serialization plugin is applied in shared/build.gradle.kts.
 *
 * `data object` rather than `data class` because none of these carry arguments yet. When one
 * does — a single AirBeam by id, say — it becomes `data class AirBeam(val id: String)` and the
 * argument travels type-safely, no Bundle plumbing and no string route template.
 */
@Serializable
sealed interface SettingsRoute {
  @Serializable data object Root : SettingsRoute
  @Serializable data object Account : SettingsRoute
  @Serializable data object AirBeams : SettingsRoute
  @Serializable data object AppSettings : SettingsRoute
  @Serializable data object Help : SettingsRoute

  companion object {
    /** The four rows on the Settings root, in Figma order (161:35267 → 161:35309). */
    val sections: List<SettingsRoute> = listOf(Account, AirBeams, AppSettings, Help)
  }
}

/**
 * App-bar title for every destination. Exhaustive `when` over the sealed interface, so adding
 * a destination is a compile error until it is given a title.
 */
fun Strings.title(route: SettingsRoute): String = when (route) {
  SettingsRoute.Root -> settingsTitle
  SettingsRoute.Account -> settingsAccountTitle
  SettingsRoute.AirBeams -> settingsAirBeamsTitle
  SettingsRoute.AppSettings -> settingsAppTitle
  SettingsRoute.Help -> settingsHelpTitle
}

/** Supporting line under each root row. `null` for [SettingsRoute.Root] — it *is* the list. */
fun Strings.subtitle(route: SettingsRoute): String? = when (route) {
  SettingsRoute.Root -> null
  SettingsRoute.Account -> settingsAccountSubtitle
  SettingsRoute.AirBeams -> settingsAirBeamsSubtitle
  SettingsRoute.AppSettings -> settingsAppSubtitle
  SettingsRoute.Help -> settingsHelpSubtitle
}

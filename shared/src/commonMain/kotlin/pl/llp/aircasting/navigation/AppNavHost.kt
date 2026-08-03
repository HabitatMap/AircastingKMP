package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import pl.llp.aircasting.settings.SettingsPlaceholderScreen
import pl.llp.aircasting.settings.SettingsRootScreen

/** The tab shell — bottom bar plus all five tabs. */
@Serializable
data object ShellRoute

/**
 * The root back stack.
 *
 * Settings sits *beside* [ShellRoute], not inside a tab, because the design has no bottom bar
 * on any Settings screen. Because [AircastingNavBar] lives inside the shell destination, it
 * disappears on Settings for free — no `if (currentRoute is ...)` conditional anywhere.
 *
 * Deliberately no `enterTransition`/`exitTransition`: on iOS a custom transition suppresses
 * the default back-swipe animation that Compose Multiplatform gives us for nothing.
 */
@Composable
fun AppNavHost(onRequestLocation: () -> Unit = {}) {
  val nav = rememberNavController()
  NavHost(navController = nav, startDestination = ShellRoute) {
    composable<ShellRoute> {
      ShellScreen(
        onRequestLocation = onRequestLocation,
        onOpenSettings = { nav.navigate(SettingsRoute.Root) },
      )
    }
    settingsGraph(nav, onExit = { nav.popBackStack() })
  }
}

/**
 * The Settings destinations, factored out so iOS can host them in a NavHost of their own
 * (see SettingsViewController) without duplicating the wiring.
 *
 * [onExit] is what "back" does at [SettingsRoute.Root]: on Android it pops back to the shell;
 * on iOS it dismisses the full-screen cover that Swift presented.
 */
internal fun NavGraphBuilder.settingsGraph(nav: NavHostController, onExit: () -> Unit) {
  val pop: () -> Unit = { nav.popBackStack() }

  composable<SettingsRoute.Root> {
    SettingsRootScreen(onBack = onExit, onSection = { nav.navigate(it) })
  }

  // composable<T> takes a reified type parameter, so these can't be generated in a loop over
  // SettingsRoute.sections. NOTE: nothing enforces that this list stays in sync with
  // `sections` — a new section without a `composable<>` here fails at navigate() time, not at
  // compile time. See the review note about closing that gap.
  composable<SettingsRoute.Account> { SettingsPlaceholderScreen(SettingsRoute.Account, pop) }
  composable<SettingsRoute.AirBeams> { SettingsPlaceholderScreen(SettingsRoute.AirBeams, pop) }
  composable<SettingsRoute.AppSettings> { SettingsPlaceholderScreen(SettingsRoute.AppSettings, pop) }
  composable<SettingsRoute.Help> { SettingsPlaceholderScreen(SettingsRoute.Help, pop) }
}

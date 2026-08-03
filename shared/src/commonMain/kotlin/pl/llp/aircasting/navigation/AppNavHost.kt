package pl.llp.aircasting.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import pl.llp.aircasting.settings.SettingsPlaceholderScreen
import pl.llp.aircasting.settings.SettingsRootScreen

@Serializable
data object ShellRoute

@Composable
fun AppNavHost(onRequestLocation: () -> Unit = {}) {
  val nav = rememberNavController()
  NavHost(
    navController = nav,
    startDestination = ShellRoute,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition,
  ) {
    composable<ShellRoute> {
      ShellScreen(
        onRequestLocation = onRequestLocation,
        onOpenSettings = { nav.navigateOnce(SettingsRoute.Root) },
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
    SettingsRootScreen(onBack = onExit, onSection = { nav.navigateOnce(it) })
  }

  composable<SettingsRoute.Account> { SettingsPlaceholderScreen(SettingsRoute.Account, pop) }
  composable<SettingsRoute.AirBeams> { SettingsPlaceholderScreen(SettingsRoute.AirBeams, pop) }
  composable<SettingsRoute.AppSettings> { SettingsPlaceholderScreen(SettingsRoute.AppSettings, pop) }
  composable<SettingsRoute.Help> { SettingsPlaceholderScreen(SettingsRoute.Help, pop) }
}

private fun NavController.navigateOnce(route: Any) {
  if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

private const val NavDurationMs = 300

private val NavSpec: FiniteAnimationSpec<IntOffset> =
  tween(durationMillis = NavDurationMs, easing = FastOutSlowInEasing)

val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
  slideInHorizontally(initialOffsetX = { it }, animationSpec = NavSpec)
}

val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
  slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = NavSpec)
}
val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
  slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = NavSpec)
}

val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
  slideOutHorizontally(targetOffsetX = { it }, animationSpec = NavSpec)
}

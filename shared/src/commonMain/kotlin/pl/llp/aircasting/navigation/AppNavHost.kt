package pl.llp.aircasting.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import pl.llp.aircasting.auth.AuthGate
import pl.llp.aircasting.auth.AuthRepository
import pl.llp.aircasting.data.auth.isSignedIn
import pl.llp.aircasting.settings.SettingsPlaceholderScreen
import pl.llp.aircasting.settings.SettingsRootScreen
import pl.llp.aircasting.settings.account.SettingsAccountScreen

@Serializable
data object ShellRoute

@Composable
fun AppNavHost(onRequestLocation: () -> Unit = {}) {
  val nav = rememberNavController()
  val auth = koinInject<AuthRepository>()
  val session by auth.state.collectAsStateWithLifecycle()
  val startedSignedIn = remember(auth) { auth.state.value.isSignedIn }
  val startDestination: Any = if (startedSignedIn) ShellRoute else AuthRoute
  var wasSignedIn by remember { mutableStateOf(startedSignedIn) }
  LaunchedEffect(session) {
    val signedIn = session.isSignedIn
    if (signedIn == wasSignedIn) return@LaunchedEffect
    wasSignedIn = signedIn
    nav.navigate(if (signedIn) ShellRoute else AuthRoute) { popUpTo(0) { inclusive = true } }
  }

  NavHost(
    navController = nav,
    startDestination = startDestination,
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
    composable<AuthRoute> {
      AuthDestination(onForgotPassword = { nav.navigateOnce(ForgotPasswordRoute) })
    }
    composable<ForgotPasswordRoute> { ForgotPasswordDestination(onBack = { nav.popBackStack() }) }

    settingsGraph(nav)
  }
}

private fun NavGraphBuilder.settingsGraph(nav: NavHostController) {
  val pop: () -> Unit = { nav.popBackStack() }

  composable<SettingsRoute.Root> {
    SettingsRootScreen(onBack = pop, onSection = { nav.navigateOnce(it) })
  }
  composable<SettingsRoute.Account> {
    AuthGate(
      onCancelled = pop,
      onForgotPassword = { nav.navigateOnce(ForgotPasswordRoute) },
    ) {
      AccountRoute(onBack = pop)
    }
  }

  composable<SettingsRoute.AirBeams> { SettingsPlaceholderScreen(SettingsRoute.AirBeams, pop) }
  composable<SettingsRoute.Help> { SettingsPlaceholderScreen(SettingsRoute.Help, pop) }
  composable<SettingsRoute.AppSettings> {
    AppSettingsRoute(onBack = pop, onOpenCustomServer = { nav.navigateOnce(SettingsRoute.CustomDataServer) })
  }
  composable<SettingsRoute.CustomDataServer> { CustomDataServerRoute(onExit = pop) }

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

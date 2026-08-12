package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import pl.llp.aircasting.onboarding.OnboardingRepository
import pl.llp.aircasting.onboarding.OnboardingScreen

@Serializable
data object OnboardingRoute

@Composable
fun OnboardingDestination() {
  val onboarding = koinInject<OnboardingRepository>()
  OnboardingScreen(onFinish = onboarding::complete)
}
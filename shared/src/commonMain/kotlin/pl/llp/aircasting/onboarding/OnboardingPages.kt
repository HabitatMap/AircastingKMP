package pl.llp.aircasting.onboarding

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.img_onboarding_airbeam
import org.jetbrains.compose.resources.DrawableResource
import pl.llp.aircasting.i18n.Strings

data class OnboardingPage(
  val image: DrawableResource,
  val title: String,
  val body: String,
)

fun onboardingPages(strings: Strings): List<OnboardingPage> = listOf(
  OnboardingPage(
    image = Res.drawable.img_onboarding_airbeam,
    title = strings.onboardingMeasureTitle,
    body = strings.onboardingMeasureBody,
  ),
  OnboardingPage(
    image = Res.drawable.img_onboarding_airbeam,
    title = strings.onboardingMapTitle,
    body = strings.onboardingMapBody,
  ),
  OnboardingPage(
    image = Res.drawable.img_onboarding_airbeam,
    title = strings.onboardingShareTitle,
    body = strings.onboardingShareBody,
  ),
)
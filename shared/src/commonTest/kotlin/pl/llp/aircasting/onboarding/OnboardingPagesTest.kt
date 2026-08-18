package pl.llp.aircasting.onboarding

import pl.llp.aircasting.i18n.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingPagesTest {

  @Test
  fun every_locale_fills_all_three_pages() {
    AppStrings.forEach { (tag, strings) ->
      val pages = onboardingPages(strings)
      assertEquals(3, pages.size, "wrong page count for $tag")
      pages.forEach {
        assertTrue(it.title.isNotBlank() && it.body.isNotBlank(), "blank copy in $tag")
      }
    }
  }
}
package pl.llp.aircasting.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class AppEntryTest {

  @Test
  fun a_fresh_install_starts_at_onboarding() {
    assertEquals(AppEntry.Onboarding, appEntry(onboarded = false, signedIn = false))
  }

  @Test
  fun after_onboarding_a_signed_out_user_lands_on_auth() {
    assertEquals(AppEntry.Auth, appEntry(onboarded = true, signedIn = false))
  }

  @Test
  fun a_signed_in_user_goes_straight_to_the_shell() {
    assertEquals(AppEntry.Shell, appEntry(onboarded = true, signedIn = true))
  }
  @Test
  fun an_existing_session_counts_as_having_onboarded() {
    assertEquals(AppEntry.Shell, appEntry(onboarded = false, signedIn = true))
  }
}
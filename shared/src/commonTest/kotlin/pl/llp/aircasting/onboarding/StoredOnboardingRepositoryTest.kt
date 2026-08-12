package pl.llp.aircasting.onboarding

import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StoredOnboardingRepositoryTest {
  @Test
  fun a_fresh_install_has_not_seen_onboarding() {
    assertFalse(StoredOnboardingRepository(MapSettings()).completed.value)
  }

  @Test
  fun completing_it_survives_a_process_restart() {
    val settings = MapSettings()
    StoredOnboardingRepository(settings).complete()

    assertTrue(StoredOnboardingRepository(settings).completed.value)
  }

  @Test
  fun observers_see_the_completion() = runTest {
    val onboarding = StoredOnboardingRepository(MapSettings())

    onboarding.completed.test {
      assertFalse(awaitItem())
      onboarding.complete()
      assertTrue(awaitItem())
    }
  }
}
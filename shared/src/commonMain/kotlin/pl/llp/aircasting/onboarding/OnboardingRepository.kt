package pl.llp.aircasting.onboarding

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface OnboardingRepository {
  val completed: StateFlow<Boolean>
  fun complete()
}

class StoredOnboardingRepository(private val settings: Settings) : OnboardingRepository {

  private val _completed = MutableStateFlow(settings.getBoolean(KeyCompleted, false))
  override val completed: StateFlow<Boolean> = _completed.asStateFlow()

  override fun complete() {
    settings.putBoolean(KeyCompleted, true)
    _completed.value = true
  }
}

private const val KeyCompleted = "onboarding_completed"
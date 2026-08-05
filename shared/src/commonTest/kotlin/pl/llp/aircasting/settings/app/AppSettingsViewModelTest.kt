package pl.llp.aircasting.settings.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsViewModelTest {
  @Test
  fun `each switch writes its own field and nothing else`() {
    val repo = FakeAppSettingsRepository()
    val vm = AppSettingsViewModel(repo)

    vm.toggle(AppSetting.CrowdMap, false)
    vm.toggle(AppSetting.PushNotifications, false)
    vm.toggle(AppSetting.WifiOnlySync, true)

    assertEquals(
      AppPreferences(
        crowdMapEnabled = false,
        pushNotificationsEnabled = false,
        wifiOnlySync = true
      ),
      repo.preferences.value,
    )
  }

  @Test
  fun `turning Disable mapping on stops location tracking`() {
    val repo = FakeAppSettingsRepository()
    val vm = AppSettingsViewModel(repo)

    vm.toggle(AppSetting.DisableMapping, true)

    assertEquals(false, repo.preferences.value.locationTrackingEnabled)
  }

  @Test
  fun `touching dark mode pins it instead of leaving it on the system`() {
    // The unset value is null, and `false` is a different thing from "not chosen": once the
    // user flips the switch off, a device switching to dark must not drag the app with it.
    val repo = FakeAppSettingsRepository()
    val vm = AppSettingsViewModel(repo)

    vm.toggle(AppSetting.DarkMode, false)

    assertEquals(false, repo.preferences.value.darkMode)
  }

  @Test
  fun `a row that is not a switch cannot be toggled`() {
    // onToggle is keyed by AppSetting, so a mis-wired row would otherwise fall through to a
    // silent write. The `when` must be exhaustive and inert for these.
    val repo = FakeAppSettingsRepository()
    val vm = AppSettingsViewModel(repo)

    vm.toggle(AppSetting.TemperatureUnits, true)
    vm.toggle(AppSetting.MicrophoneCalibration, true)

    assertEquals(AppPreferences(), repo.preferences.value)
    assertEquals(0, repo.writes)
  }
}
private class FakeAppSettingsRepository : AppSettingsRepository {
  private val _preferences = MutableStateFlow(AppPreferences())
  override val preferences: StateFlow<AppPreferences> = _preferences
  var writes = 0

  override fun update(transform: (AppPreferences) -> AppPreferences) {
    val next = transform(_preferences.value)
    if (next != _preferences.value) writes++
    _preferences.value = next
  }
}

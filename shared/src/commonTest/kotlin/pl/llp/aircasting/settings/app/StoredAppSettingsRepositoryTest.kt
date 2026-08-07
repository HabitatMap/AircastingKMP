package pl.llp.aircasting.settings.app

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredAppSettingsRepositoryTest {
  @Test
  fun `a first launch reads the design defaults`() {
    // These are the states drawn in Figma 163:12617, and they are what a new user gets.
    assertEquals(
      AppPreferences(
        crowdMapEnabled = true,
        locationTrackingEnabled = true,
        darkMode = null,
        pushNotificationsEnabled = true,
        wifiOnlySync = false,
        temperatureUnit = TemperatureUnit.Fahrenheit,
        regionalFormat = RegionalFormat.US,
        mapType = MapType.Default,
      ),
      StoredAppSettingsRepository(MapSettings()).preferences.value,
    )
  }
  @Test
  fun `writes survive a restart`() {
    val store = MapSettings()
    StoredAppSettingsRepository(store).update {
      it.copy(wifiOnlySync = true, temperatureUnit = TemperatureUnit.Celsius, darkMode = true)
    }

    val reopened = StoredAppSettingsRepository(store).preferences.value

    assertEquals(true, reopened.wifiOnlySync)
    assertEquals(TemperatureUnit.Celsius, reopened.temperatureUnit)
    assertEquals(true, reopened.darkMode)
  }
  @Test
  fun `an unset dark mode stays unset after other writes`() {
    // null is persisted as "absent", not as false — otherwise the first unrelated write would
    // silently pin the theme to light.
    val store = MapSettings()
    StoredAppSettingsRepository(store).update { it.copy(wifiOnlySync = true) }

    assertEquals(null, StoredAppSettingsRepository(store).preferences.value.darkMode)
  }

  @Test
  fun `an unreadable enum falls back to its default`() {
    val store = MapSettings().apply { putString("map_type", "Hologram") }

    assertEquals(MapType.Default, StoredAppSettingsRepository(store).preferences.value.mapType)
  }

  @Test
  fun `the language choice survives a restart`() {
    val store = MapSettings()
    StoredAppSettingsRepository(store).update { it.copy(language = "fr") }

    assertEquals("fr", StoredAppSettingsRepository(store).preferences.value.language)
  }

  @Test
  fun `going back to the system language clears the stored tag`() {
    val store = MapSettings()
    StoredAppSettingsRepository(store).update { it.copy(language = "fr") }

    StoredAppSettingsRepository(store).update { it.copy(language = null) }

    assertEquals(null, StoredAppSettingsRepository(store).preferences.value.language)
  }

  @Test
  fun `a first launch starts at the default microphone offset`() {
    assertEquals(
      100,
      StoredAppSettingsRepository(MapSettings()).preferences.value.microphoneCalibrationOffset,
    )
  }

  @Test
  fun `the microphone offset survives a restart`() {
    val store = MapSettings()
    StoredAppSettingsRepository(store).update { it.copy(microphoneCalibrationOffset = 92) }

    assertEquals(
      92,
      StoredAppSettingsRepository(store).preferences.value.microphoneCalibrationOffset,
    )
  }
}
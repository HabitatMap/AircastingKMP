package pl.llp.aircasting.bluetooth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.uuid.Uuid

/** Configs get logged; a data class `toString()` would print the credentials into Logcat. */
class SessionConfigTest {

  @Test
  fun fixed_wifi_config_does_not_print_its_credentials() {
    val text = SessionConfig.FixedWiFi(
      uuid = Uuid.random(),
      authToken = "secret-token",
      latitude = 1.0,
      longitude = 2.0,
      ssid = "home",
      password = "hunter2",
      zoneOffset = 0,
    ).toString()

    assertFalse("hunter2" in text, text)
    assertFalse("secret-token" in text, text)
  }

  @Test
  fun cellular_config_does_not_print_its_token() {
    val text = SessionConfig.FixedCellular(Uuid.random(), "secret-token", 1.0, 2.0).toString()

    assertFalse("secret-token" in text, text)
  }

  @Test
  fun mobile_config_does_not_print_its_token() {
    val text = SessionConfig.Mobile(Uuid.random(), authToken = "secret-token").toString()

    assertFalse("secret-token" in text, text)
  }
}

package pl.llp.aircasting.recording

import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FakeAirBeamLink
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Fixed start: register on the backend, then configure the AirBeam (DOMAIN_KNOWLEDGE.md "Starting a fixed session"). */
class FixedSessionStarterTest {

  private val link = FakeAirBeamLink()
  private val registered = mutableListOf<NewFixedSession>()
  private var registration: RegisteredFixedSession? = REGISTERED
  private var createFails = false

  private val starter = FixedSessionStarter(link) { session ->
    registered += session
    if (createFails) error("500 from the backend")
    registration
  }

  @Test
  fun the_device_gets_the_registered_session_its_token_and_sensor_type_ids() = runTest {
    val result = starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI)

    assertEquals(FixedStartResult.Started, result)
    val session = registered.single()
    assertEquals(NewFixedSession(session.uuid, "Rooftop", listOf("home"), MINI_V2, OUTDOOR), session)
    val config = link.configured.single() as SessionConfig.FixedWiFi
    assertEquals(session.uuid, config.uuid)
    assertEquals(TOKEN, config.authToken)
    assertEquals("HomeNet" to "secret", config.ssid to config.password)
    assertEquals(50.06 to 19.94, config.latitude to config.longitude)
    assertEquals(7 to 8, config.pm1Index to config.pm25Index)
  }

  @Test
  fun every_start_registers_a_fresh_uuid() = runTest {
    link.configureResult = ConfigResult.WifiPassFailure

    starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI)
    starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI)

    assertNotEquals(registered[0].uuid, registered[1].uuid)
  }

  @Test
  fun success_drops_the_link_so_the_airbeam_runs_on_its_own() = runTest {
    starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI)

    assertEquals(1, link.disconnects)
  }

  @Test
  fun a_device_failure_is_reported_and_keeps_the_link_for_a_retry() = runTest {
    link.configureResult = ConfigResult.WifiPassFailure

    val result = starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI)

    assertEquals(FixedStartResult.DeviceFailed(ConfigResult.WifiPassFailure), result)
    assertEquals(0, link.disconnects)
  }

  @Test
  fun signed_out_never_touches_the_device() = runTest {
    registration = null

    assertEquals(FixedStartResult.SignedOut, starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI))
    assertTrue(link.configured.isEmpty())
  }

  @Test
  fun a_failed_create_never_touches_the_device() = runTest {
    createFails = true

    assertEquals(FixedStartResult.ServerError, starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI))
    assertTrue(link.configured.isEmpty())
  }

  @Test
  fun pm_ids_are_looked_up_by_canonical_name_with_no_fallback() = runTest {
    // What legacy looked up — v3 never answers with these names.
    registration = REGISTERED.copy(sensorTypeIds = mapOf("AirBeamMini-PM1" to 7, "AirBeamMini-PM2.5" to 8))

    assertEquals(FixedStartResult.ServerError, starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI))
    assertTrue(link.configured.isEmpty())
  }

  @Test
  fun only_mini_v2_starts_fixed_for_now() = runTest {
    val ab3 = DiscoveredAirBeam(DeviceId("11:22:33:44:55:66"), "AirBeam3:246f28", AirBeamDevice.AirBeam3)

    val result = starter.start(ab3, DETAILS, OUTDOOR, WIFI)

    assertEquals(FixedStartResult.DeviceFailed(ConfigResult.BadConfigFailure), result)
    assertTrue(registered.isEmpty())
  }

  private companion object {
    const val TOKEN = "0123456789abcdef0123456789abcdef"
    val MINI_V2 = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2)
    val DETAILS = NewSessionDetails("Rooftop", listOf("home"))
    val OUTDOOR = Placement.Outdoor(GeoLocation(latitude = 50.06, longitude = 19.94))
    val WIFI = WifiCredentials("HomeNet", "secret")
    val REGISTERED = RegisteredFixedSession(TOKEN, mapOf("AirBeam-PM1" to 7, "AirBeam-PM2.5" to 8))
  }
}
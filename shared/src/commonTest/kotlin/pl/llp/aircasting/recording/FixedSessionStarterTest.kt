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

  private val starter = FixedSessionStarter(link, zoneOffsetHours = { 2 }) { session ->
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
  /** V1 firmware uploads to the realtime API as the user, so it gets the user's token, not v3's session token. */
  @Test
  fun an_ab3_uploads_as_the_user_with_the_phones_zone_offset() = runTest {
    val result = starter.start(AB3, DETAILS, OUTDOOR, WIFI)

    assertEquals(FixedStartResult.Started, result)
    val config = link.configured.single() as SessionConfig.FixedWiFi
    assertEquals(registered.single().uuid, config.uuid)
    assertEquals(USER_TOKEN, config.authToken)
    assertEquals(2, config.zoneOffset)
    assertEquals("HomeNet" to "secret", config.ssid to config.password)
    assertEquals(1, link.disconnects)
  }

  /** V1 frames carry sensor names: an answer without sensor_type_ids is fine. */
  @Test
  fun an_ab3_needs_no_sensor_type_ids() = runTest {
    registration = REGISTERED.copy(sensorTypeIds = emptyMap())

    assertEquals(FixedStartResult.Started, starter.start(AB3, DETAILS, OUTDOOR, WIFI))
  }

  @Test
  fun an_ab2_on_cellular_needs_no_wifi() = runTest {
    val result = starter.start(AB2, DETAILS, OUTDOOR, Uplink.Cellular)

    assertEquals(FixedStartResult.Started, result)
    val config = link.configured.single() as SessionConfig.FixedCellular
    assertEquals(USER_TOKEN, config.authToken)
    assertEquals(50.06 to 19.94, config.latitude to config.longitude)
  }

  /** Cellular is AB2/AB3 only (decision 2026-10-26); refused before an empty session is registered. */
  @Test
  fun a_mini_on_cellular_is_refused_before_anything_is_registered() = runTest {
    val result = starter.start(MINI_V2, DETAILS, OUTDOOR, Uplink.Cellular)

    assertEquals(FixedStartResult.DeviceFailed(ConfigResult.BadConfigFailure), result)
    assertTrue(registered.isEmpty())
  }

  @Test
  fun pm_ids_are_found_under_the_names_we_sent() = runTest {
    // What the live v3 backend answers with (and what legacy looked up).
    registration = REGISTERED.copy(sensorTypeIds = mapOf("AirBeamMini-PM1" to 7, "AirBeamMini-PM2.5" to 8))

    assertEquals(FixedStartResult.Started, starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI))
    val config = link.configured.single() as SessionConfig.FixedWiFi
    assertEquals(7 to 8, config.pm1Index to config.pm25Index)
  }

  @Test
  fun a_missing_pm_id_fails_before_the_device_is_touched() = runTest {
    registration = REGISTERED.copy(sensorTypeIds = mapOf("AirBeamMini-PM1" to 7))

    assertEquals(FixedStartResult.ServerError, starter.start(MINI_V2, DETAILS, OUTDOOR, WIFI))
    assertTrue(link.configured.isEmpty())
  }

  private companion object {
    const val TOKEN = "0123456789abcdef0123456789abcdef"
    val MINI_V2 = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2)
    val DETAILS = NewSessionDetails("Rooftop", listOf("home"))
    val OUTDOOR = Placement.Outdoor(GeoLocation(latitude = 50.06, longitude = 19.94))
    const val USER_TOKEN = "user-token"
    val WIFI = Uplink.Wifi(WifiCredentials("HomeNet", "secret"))
    val REGISTERED = RegisteredFixedSession(TOKEN, mapOf("AirBeam-PM1" to 7, "AirBeam-PM2.5" to 8), USER_TOKEN)
    val AB3 = DiscoveredAirBeam(DeviceId("11:22:33:44:55:66"), "AirBeam3:246f28", AirBeamDevice.AirBeam3)
    val AB2 = DiscoveredAirBeam(DeviceId("00:18:96:10:71:9F"), "AirBeam2:00189610719F", AirBeamDevice.AirBeam2)
  }
}
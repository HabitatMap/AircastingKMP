package pl.llp.aircasting.bluetooth.v2_firmware_specific

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse.Ack
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse.Nack
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse.Ready
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.NackErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Mini V2 fixed start (ble_mobile_app_guide.md §D, legacy `AirBeamMiniV2Configurator`): the first
 * Ready means the first measurement reached the backend. Virtual time drives the 30 s limit.
 */
class FixedStartVerdictTest {

  private fun replies(vararg steps: Pair<Duration, CommandResponse>) = flow {
    steps.forEach { (after, reply) -> delay(after); emit(reply) }
  }

  @Test
  fun first_Ready_is_success() = runTest {
    assertEquals(ConfigResult.Success, awaitFixedStart(replies(0.seconds to Ack, 5.seconds to Ready)))
  }

  @Test
  fun Ready_just_inside_the_limit_is_success() = runTest {
    assertEquals(ConfigResult.Success, awaitFixedStart(replies(0.seconds to Ack, 29.seconds to Ready)))
  }

  @Test
  fun no_Ready_within_30s_fails_and_stops_waiting_at_30s() = runTest {
    val result = awaitFixedStart(replies(0.seconds to Ack, 31.seconds to Ready))

    assertEquals(ConfigResult.FirstMeasurementFailure, result)
    assertEquals(30_000, currentTime)
  }

  @Test
  fun wrong_wifi_credentials_fail_as_WifiPassFailure() = runTest {
    val result = awaitFixedStart(replies(0.seconds to Ack, 3.seconds to Nack(NackErrorCode.InvalidWifiCredentials)))

    assertEquals(ConfigResult.WifiPassFailure, result)
  }

  @Test
  fun InvalidConfig_after_fixed_config_means_the_first_measurement_POST_failed() = runTest {
    val result = awaitFixedStart(replies(0.seconds to Ack, 10.seconds to Nack(NackErrorCode.InvalidConfig)))

    assertEquals(ConfigResult.FirstMeasurementFailure, result)
  }
}
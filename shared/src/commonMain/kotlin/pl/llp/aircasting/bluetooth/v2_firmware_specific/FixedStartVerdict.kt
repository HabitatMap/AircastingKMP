package pl.llp.aircasting.bluetooth.v2_firmware_specific

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse.Nack
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.CommandResponse.Ready
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol.MiniV2.NackErrorCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long a freshly configured fixed session has to POST its first measurement. */
internal val FixedStartTimeout = 30.seconds

/**
 * Verdict on a Mini V2 fixed `NewSessionConfig` (ble_mobile_app_guide.md §D). Ack only means
 * "understood"; the first Ready means the first measurement reached the backend. Stops at the
 * first verdict, so the per-measurement heartbeat Readys that follow are never seen.
 */
internal suspend fun awaitFixedStart(
  responses: Flow<CommandResponse>,
  timeout: Duration = FixedStartTimeout,
): ConfigResult {
  val verdict = withTimeoutOrNull(timeout) { responses.first { it is Ready || it is Nack } }
  return when (verdict) {
    is Ready -> ConfigResult.Success
    is Nack ->
      if (verdict.errorCode == NackErrorCode.InvalidConfig) ConfigResult.FirstMeasurementFailure
      else verdict.errorCode.toConfigResult()
    else -> ConfigResult.FirstMeasurementFailure // timed out
  }
}

internal fun NackErrorCode.toConfigResult(): ConfigResult = when (this) {
  NackErrorCode.InvalidConfig -> ConfigResult.BadConfigFailure
  NackErrorCode.InvalidWifiCredentials -> ConfigResult.WifiPassFailure
  NackErrorCode.StorageHasMeasurements -> ConfigResult.NotSyncedFailure
  else -> ConfigResult.UnknownFailure
}
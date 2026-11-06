package pl.llp.aircasting.record

import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.i18n.Strings

/** What to tell the user when Start didn't work; null while nothing has failed. */
fun Strings.startFailure(state: StartState): String? = when (state) {
  StartState.SignedOut -> startFailedSignedOut
  StartState.ServerError -> startFailedServer
  is StartState.Failed -> when (state.result) {
    ConfigResult.WifiPassFailure -> startFailedWifi
    ConfigResult.FirstMeasurementFailure -> startFailedUpload
    ConfigResult.NotConnected -> startFailedNotConnected
    else -> startFailedGeneric
  }
  StartState.Idle, StartState.Started, is StartState.Preparing -> null
}
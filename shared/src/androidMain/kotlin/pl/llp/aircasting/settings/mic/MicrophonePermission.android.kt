package pl.llp.aircasting.settings.mic

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
  val context = LocalContext.current
  // The MutableState (not its value) is captured, so `granted` re-reads it on every recomposition
  // and the object survives the grant flipping.
  val state = remember { mutableStateOf(context.hasRecordAudioPermission()) }
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { state.value = it }

  return remember(launcher) {
    object : MicrophonePermission {
      override val granted get() = state.value
      override fun request() = launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }
}
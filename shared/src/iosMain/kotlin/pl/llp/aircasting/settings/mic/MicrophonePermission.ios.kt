package pl.llp.aircasting.settings.mic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberMicrophonePermission(): MicrophonePermission {
  val session = remember { AVAudioSession.sharedInstance() }
  val state = remember {
    mutableStateOf(session.recordPermission == AVAudioSessionRecordPermissionGranted)
  }

  return remember(session) {
    object : MicrophonePermission {
      override val granted get() = state.value
      override fun request() {
        session.requestRecordPermission { ok ->
          // The callback lands on an arbitrary queue; snapshot state must be written on main.
          dispatch_async(dispatch_get_main_queue()) { state.value = ok }
        }
      }
    }
  }
}
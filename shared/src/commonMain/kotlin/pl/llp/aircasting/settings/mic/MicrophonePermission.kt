package pl.llp.aircasting.settings.mic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Read-and-request access to the microphone.
 *
 * `@Stable` because [isGranted] is backed by snapshot state on both platforms: Compose may skip
 * recomposition of anything holding this until that state actually changes.
 */
@Stable
interface MicrophonePermission {
  val isGranted: Boolean
  fun request()
}

@Composable
expect fun rememberMicrophonePermission(): MicrophonePermission
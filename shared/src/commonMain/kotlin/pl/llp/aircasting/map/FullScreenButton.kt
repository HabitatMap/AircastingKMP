package pl.llp.aircasting.map

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_fullscreen
import aircasting.shared.generated.resources.ic_fullscreen_exit
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun FullScreenButton(fullScreen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  MapButton(
    if (fullScreen) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen,
    if (fullScreen) strings.mapExitFullScreen else strings.mapFullScreen,
    onClick,
    modifier,
  )
}

/** A round control floating over a map (full screen, Centre). */
@Composable
fun MapButton(icon: DrawableResource, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    onClick = onClick,
    modifier = modifier.size(36.dp),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 2.dp,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(painterResource(icon), contentDescription, tint = MaterialTheme.colorScheme.onSurface)
    }
  }
}

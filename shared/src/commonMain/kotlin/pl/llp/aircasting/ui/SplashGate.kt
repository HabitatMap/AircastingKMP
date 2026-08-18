package pl.llp.aircasting.ui

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_aircasting_wordmark
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import kotlin.time.Duration.Companion.seconds

private val SplashDuration = 1.seconds

private const val SplashFadeMillis = 400

@Composable
fun SplashGate(content: @Composable () -> Unit) {
  var elapsed by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(SplashDuration)
    elapsed = true
  }

  Box {
    if (elapsed) content()
    AnimatedVisibility(
      visible = !elapsed,
      enter = EnterTransition.None,
      exit = fadeOut(tween(SplashFadeMillis)),
    ) {
      SplashScreen()
    }
  }
}

@Composable
private fun SplashScreen() {
  Box(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painterResource(Res.drawable.ic_aircasting_wordmark),
      contentDescription = LocalStrings.current.appLogo,
      tint = Color.White,
      modifier = Modifier.width(281.dp).aspectRatio(113f / 32f),
    )
  }
}

package pl.llp.aircasting.auth

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_aircasting_wordmark
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

private val ScreenPadding = 24.dp

@Composable
fun AuthScreen(state: AuthFormState, actions: AuthFormActions) {
  val strings = LocalStrings.current
  Column(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .navigationBarsPadding()
      .imePadding()
      .verticalScroll(rememberScrollState()),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      shadowElevation = 3.dp,          // Figma "M3/Elevation Light/1"
    ) {
      Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
          Icon(
            painterResource(Res.drawable.ic_aircasting_wordmark),
            contentDescription = strings.appLogo,
            tint = Color.Unspecified,   // the wordmark is already brand-coloured
          )
        }
        Spacer(Modifier.height(42.dp))
        AuthHeader(
          title = strings.authWelcomeTitle,
          subtitle = strings.authWelcomeSubtitle,
          titleStyle = MaterialTheme.typography.headlineLargeEmphasized,
          modifier = Modifier.padding(horizontal = ScreenPadding),
        )
        Spacer(Modifier.height(24.dp))
        AuthTabs(state.mode, actions.onSwitchMode)
      }
    }
    Spacer(Modifier.height(80.dp))
    AuthForm(state, actions, Modifier.padding(horizontal = ScreenPadding))
    Spacer(Modifier.height(32.dp))
  }
}

@Composable
private fun AuthTabs(mode: AuthMode, onSelect: (AuthMode) -> Unit) {
  val strings = LocalStrings.current
  Row(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding).height(48.dp)) {
    AuthMode.entries.forEach { tab ->
      val selected = tab == mode
      Column(
        Modifier.weight(1f).fillMaxHeight().clickable { onSelect(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(
            when (tab) {
              AuthMode.SignIn -> strings.authTabSignIn
              AuthMode.SignUp -> strings.authTabSignUp
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Box(
          Modifier.fillMaxWidth().height(2.dp).background(
            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          ),
        )
      }
    }
  }
}

package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_aircasting_wordmark
import aircasting.shared.generated.resources.ic_airbeam_add
import aircasting.shared.generated.resources.ic_airbeam_sync
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.img_record_start_hero
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun RecordStartScreen(onStartNewSession: () -> Unit, onRecoverData: () -> Unit) {
  val strings = LocalStrings.current
  Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
      painterResource(Res.drawable.ic_aircasting_wordmark),
      contentDescription = strings.appLogo,
      modifier = Modifier.padding(vertical = 16.dp).height(32.dp),
    )
    Spacer(Modifier.height(32.dp))
    Image(
      painterResource(Res.drawable.img_record_start_hero),
      contentDescription = null,
      modifier = Modifier.size(200.dp),
    )
    Spacer(Modifier.height(24.dp))
    Column(Modifier.padding(horizontal = 24.dp)) {
      Text(
        strings.recordStartTitle,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineLargeEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        strings.recordStartBody,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(48.dp))
      Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        RecordActionCard(
          icon = Res.drawable.ic_airbeam_add,
          title = strings.recordNewSessionTitle,
          body = strings.recordNewSessionBody,
          onClick = onStartNewSession,
        )
        RecordActionCard(
          icon = Res.drawable.ic_airbeam_sync,
          title = strings.recordRecoverTitle,
          body = strings.recordRecoverBody,
          onClick = onRecoverData,
        )
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun RecordActionCard(
  icon: DrawableResource,
  title: String,
  body: String,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Row(
      Modifier.padding(20.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier
          .size(54.dp)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painterResource(icon),
          contentDescription = null,
          modifier = Modifier.size(width = 23.dp, height = 27.dp),
          tint = MaterialTheme.colorScheme.primaryContainer,
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          body,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      Icon(
        painterResource(Res.drawable.ic_arrow_forward_ios),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
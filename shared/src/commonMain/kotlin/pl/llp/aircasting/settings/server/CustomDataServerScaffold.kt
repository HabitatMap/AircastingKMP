package pl.llp.aircasting.settings.server

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun CustomDataServerScaffold(
  progress: Float,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  footer: @Composable () -> Unit,
  body: @Composable () -> Unit,
) {
  val strings = LocalStrings.current
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      Column(
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) { footer() }
    },
  ) { padding ->
    Column(
      Modifier.padding(padding).padding(horizontal = 24.dp).padding(top = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        trackColor = MaterialTheme.colorScheme.surfaceTint,
      )
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          Modifier.clickable(onClick = onBack).padding(vertical = 10.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            painterResource(Res.drawable.ic_arrow_forward_ios),
            contentDescription = null,
            modifier = Modifier.size(20.dp).rotate(180f),
            tint = MaterialTheme.colorScheme.secondary,
          )
          Text(
            strings.back,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
        Text(
          strings.cancel,
          modifier = Modifier.clickable(onClick = onCancel).padding(vertical = 10.dp, horizontal = 4.dp),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      body()
    }
  }
}

@Composable
fun WizardButton(
  label: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  showChevron: Boolean = true,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.2.dp,
      MaterialTheme.colorScheme.primaryContainer
    ),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(
      horizontal = 24.dp,
      vertical = 16.dp
    ),
  ) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    if (showChevron) {
      Icon(
        painterResource(Res.drawable.ic_arrow_forward_ios),
        contentDescription = null,
        modifier = Modifier.padding(start = 8.dp)
          .size(20.dp),
      )
    }
  }
}
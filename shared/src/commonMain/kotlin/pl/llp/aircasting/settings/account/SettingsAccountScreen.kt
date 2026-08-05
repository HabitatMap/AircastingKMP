package pl.llp.aircasting.settings.account

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_person
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.SettingsRoute
import pl.llp.aircasting.navigation.title
import pl.llp.aircasting.settings.SettingsScaffold

@Composable
fun SettingsAccountScreen(
  state: AccountScreenState,
  onBack: () -> Unit,
  onAction: (AccountAction) -> Unit,
  onSignOut: () -> Unit,
  onDeleteAccount: () -> Unit,
  onDeletionConfirmed: () -> Unit,
  onDeletionCodeSubmit: (String) -> Unit,
  onDeletionDismissed: () -> Unit,
) {
  val strings = LocalStrings.current
  val content = state as? AccountScreenState.Content

  SettingsScaffold(title = strings.title(SettingsRoute.Account), onBack = onBack) { padding ->
    Column(Modifier.padding(padding).padding(horizontal = 24.dp).padding(top = 24.dp)) {
      content?.let {
        ProfileCard(it.profile)
        Spacer(Modifier.height(26.dp))
      }
      Text(
        strings.settingsAccountSectionHeader,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
      )
      Spacer(Modifier.height(16.dp))
      Column(
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerLowest)
          .padding(horizontal = 16.dp),
      ) {
        AccountAction.entries.forEach { action ->
          ActionRow(action, onClick = { onAction(action) })
        }
      }
      Spacer(Modifier.height(32.dp))
      SignOutButton(onSignOut)
      Spacer(Modifier.height(16.dp))
      DeleteAccountButton(onDeleteAccount)
    }
  }
  when (val deletion = content?.deletion) {
    Deletion.Confirming -> DeleteAccountConfirmDialog(
      onConfirm = onDeletionConfirmed,
      onDismiss = onDeletionDismissed,
    )
    is Deletion.AwaitingCode -> DeleteAccountCodeDialog(
      email = content.profile.email,
      rejected = deletion.rejected,
      onSubmit = onDeletionCodeSubmit,
      onResend = onDeletionConfirmed,
      onDismiss = onDeletionDismissed,
    )
    Deletion.None, null -> Unit
  }
}

@Composable
private fun ProfileCard(profile: AccountProfile) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painterResource(Res.drawable.ic_person),
        contentDescription = null,
        modifier = Modifier.size(21.dp),
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
    Column(Modifier.weight(1f)) {
      Text(
        profile.name,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        profile.email,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ActionRow(action: AccountAction, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      LocalStrings.current.label(action),
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Icon(
      painterResource(Res.drawable.ic_arrow_forward_ios),
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primaryContainer),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
  ) {
    Text(LocalStrings.current.accountSignOut, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun DeleteAccountButton(onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.error,
      contentColor = MaterialTheme.colorScheme.onError,
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
  ) {
    Text(LocalStrings.current.accountDeleteAccount, style = MaterialTheme.typography.titleMedium)
  }
}


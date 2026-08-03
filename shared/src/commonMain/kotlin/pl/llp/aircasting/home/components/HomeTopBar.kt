package pl.llp.aircasting.home.components

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_aircasting_wordmark
import aircasting.shared.generated.resources.ic_settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onOpenSettings: () -> Unit) {
  val strings = LocalStrings.current
  TopAppBar(
    title = {
      Image(
        painter = painterResource(Res.drawable.ic_aircasting_wordmark),
        contentDescription = strings.appLogo,
        modifier = Modifier.padding(start = 8.dp).height(32.dp),
      )
    },
    actions = {
      IconButton(onClick = onOpenSettings) {
        Icon(
          painterResource(Res.drawable.ic_settings),
          contentDescription = strings.openSettings,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
  )
}

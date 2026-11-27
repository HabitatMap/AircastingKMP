package pl.llp.aircasting.settings

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_back
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

/**
 * Back-arrow screen frame. The bar is opaque, in the screen's own background: scrolled content
 * passes under it (Scaffold draws topBar above the body) instead of showing through it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
  title: String,
  onBack: () -> Unit,
  actions: @Composable RowScope.() -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  val background = MaterialTheme.colorScheme.background
  Scaffold(
    containerColor = background,
    topBar = {
      TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              painterResource(Res.drawable.ic_arrow_back),
              contentDescription = LocalStrings.current.back,
            )
          }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = background,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
          actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    content = content,
  )
}

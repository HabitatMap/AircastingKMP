package pl.llp.aircasting.navigation

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add_circle
import aircasting.shared.generated.resources.ic_add_circle_filled
import aircasting.shared.generated.resources.ic_favorite
import aircasting.shared.generated.resources.ic_favorite_filled
import aircasting.shared.generated.resources.ic_folder
import aircasting.shared.generated.resources.ic_folder_filled
import aircasting.shared.generated.resources.ic_home
import aircasting.shared.generated.resources.ic_home_filled
import aircasting.shared.generated.resources.ic_map
import aircasting.shared.generated.resources.ic_map_filled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AircastingNavBar(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  NavigationBar(
    // Figma bar is white with an elevation-2 drop shadow. M3's tonalElevation would *tint*
    // the surface instead of casting a shadow, so switch it off and draw the shadow ourselves.
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 0.dp,
    modifier = modifier.shadow(elevation = 3.dp),
  ) {
    AppTab.entries.forEach { tab ->
      val isSelected = tab == selected
      NavigationBarItem(
        selected = isSelected,
        onClick = { onSelect(tab) },
        icon = {
          Icon(painterResource(tab.icon(isSelected)), contentDescription = null)
        },
        label = { Text(strings.label(tab), style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primaryContainer,
          selectedTextColor = MaterialTheme.colorScheme.primaryContainer,
          indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      )
    }
  }
}

private fun AppTab.icon(selected: Boolean): DrawableResource = when (this) {
  AppTab.Home -> if (selected) Res.drawable.ic_home_filled else Res.drawable.ic_home
  AppTab.Explore -> if (selected) Res.drawable.ic_map_filled else Res.drawable.ic_map
  AppTab.Record -> if (selected) Res.drawable.ic_add_circle_filled else Res.drawable.ic_add_circle
  AppTab.Favorites -> if (selected) Res.drawable.ic_favorite_filled else Res.drawable.ic_favorite
  AppTab.MyData -> if (selected) Res.drawable.ic_folder_filled else Res.drawable.ic_folder
}

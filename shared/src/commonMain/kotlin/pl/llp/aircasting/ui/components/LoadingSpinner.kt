package pl.llp.aircasting.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's one circular loading indicator (the custom-server "Testing connection" look).
 * Use this instead of a raw [CircularProgressIndicator] so every spinner matches.
 * The stroke scales with [size] (5dp at the reference 44dp), never thinner than 2dp.
 */
@Composable
fun LoadingSpinner(modifier: Modifier = Modifier, size: Dp = 44.dp) {
  CircularProgressIndicator(
    modifier = modifier.size(size),
    color = MaterialTheme.colorScheme.primaryContainer,
    trackColor = MaterialTheme.colorScheme.inversePrimary,
    strokeWidth = (size * 5f / 44f).coerceAtLeast(2.dp),
  )
}

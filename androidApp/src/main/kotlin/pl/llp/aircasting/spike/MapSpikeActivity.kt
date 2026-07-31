package pl.llp.aircasting.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 *
 * Throwaway host for the map rendering spike — separate launcher entry so it stays out of the
 * real app flow. Delete this package (and its manifest entry) once the question is answered.
 */
class MapSpikeActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MapSpikeScreen() }
  }
}

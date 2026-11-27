package pl.llp.aircasting.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.format.LocalDateFormatter
import pl.llp.aircasting.ui.streamChipLabel
import kotlin.math.roundToInt

/** "PM 2.5: 8 µg/m³": the stream named like its details chip, the reading rounded like the stats. */
internal fun noteValueLabel(sensorName: String, value: Double?, unit: String): String? =
  value?.let { "${streamChipLabel(sensorName)}: ${it.roundToInt()} $unit" }

/**
 * Figma "Note card" (1246:410): text, then date on the session's clock and the selected stream's
 * reading at the note's time. [level] null (no threshold, no reading) → no dot.
 */
@Composable
fun NoteCard(
  item: NoteOnStream,
  sensorName: String,
  unit: String,
  level: MeasurementLevel?,
  timeZone: TimeZone,
  modifier: Modifier = Modifier,
  /** Null: no photo, or still decoding. */
  photo: ImageBitmap? = null,
) {
  val shape = RoundedCornerShape(12.dp)
  Column(
    // Clip first, so a clickable modifier from the caller ripples inside the rounded card.
    Modifier.fillMaxWidth()
      .clip(shape)
      .then(modifier)
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
  ) {
    // Figma 1248:401: edge to edge on top, 140 dp tall.
    photo?.let {
      Image(it, LocalStrings.current.notesPhoto, Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Crop)
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(item.note.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
          LocalDateFormatter.current.dateTime(item.note.time, timeZone),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        noteValueLabel(sensorName, item.value, unit)?.let { label ->
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            level?.let { LevelDot(it, size = 8.dp) }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }
  }
}

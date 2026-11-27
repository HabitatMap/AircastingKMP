package pl.llp.aircasting.notes

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.ui.chart.ChartPoint
import pl.llp.aircasting.ui.chart.wallClockPoint

/**
 * Graph markers for [notes] as seen on one stream: on the same wall-clock x as the graph's points
 * ([wallClockPoint]) and at that stream's reading. No reading on this stream → no marker.
 */
internal fun noteMarkers(notes: List<NoteOnStream>, timeZone: TimeZone): List<ChartPoint> =
  notes.mapNotNull { (note, value) -> value?.let { wallClockPoint(note.time, it, timeZone) } }

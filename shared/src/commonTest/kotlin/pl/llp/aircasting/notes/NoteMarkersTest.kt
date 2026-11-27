package pl.llp.aircasting.notes

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Note
import pl.llp.aircasting.domain.session.NoteOnStream
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Figma "Timeline note indicator" (1147:351): each note sits on the line where its reading is. */
class NoteMarkersTest {
  private val warsaw = TimeZone.of("Europe/Warsaw")

  @Test
  fun `a marker sits at the note time on the session wall clock and at its reading`() {
    // 08:00 UTC is 10:00 in Warsaw on CEST: the chart reads wall-clock seconds as UTC.
    val time = Instant.parse("2026-10-19T08:00:00Z")
    val markers = noteMarkers(listOf(NoteOnStream(note(time), value = 13.0)), warsaw)

    assertEquals(listOf(ChartPoint(x = Instant.parse("2026-10-19T10:00:00Z").epochSeconds.toDouble(), y = 13.0)), markers)
  }

  @Test
  fun `a note with no reading on this stream gets no marker`() {
    val markers = noteMarkers(listOf(NoteOnStream(note(Instant.parse("2026-10-19T08:00:00Z")), value = null)), warsaw)

    assertEquals(emptyList(), markers)
  }

  private fun note(time: Instant) = Note(1, "s1", time, "Smells like smoke", GeoLocation(50.06, 19.94))
}

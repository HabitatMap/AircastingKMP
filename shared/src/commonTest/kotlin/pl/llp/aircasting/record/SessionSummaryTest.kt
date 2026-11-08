package pl.llp.aircasting.record

import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionSummaryTest {

  @Test
  fun `the summary is the form phrased for the confirmation card`() {
    val form = NewSessionForm(
      method = RecordingMethod.PhoneMicrophone,
      name = "Morning commute",
      committedTags = listOf("school", "traffic"),
      interval = SamplingInterval.OneSecond,
    )
    val summary = EnStrings.summarize(form)

    assertEquals("Phone Microphone", summary.type)
    assertEquals("Morning commute", summary.name)
    assertEquals(listOf("school", "traffic"), summary.tags)
    assertEquals("1 s", summary.interval)
  }
  @Test
  fun `every locale labels every sampling interval distinctly`() {
    AppStrings.forEach { (tag, strings) ->
      val labels = SamplingInterval.entries.map { strings.label(it) }
      assertTrue(labels.all { it.isNotBlank() }, "blank interval label in $tag")
      assertEquals(labels.size, labels.toSet().size, "duplicate interval label in $tag")
    }
  }
  @Test
  fun `a fixed session always samples every minute whatever the picker held`() {
    val form = NewSessionForm(method = RecordingMethod.AirBeamFixed, interval = SamplingInterval.OneSecond)

    assertEquals("1 min", EnStrings.summarize(form).interval)
  }
  @Test
  fun `a fixed session says where it stands`() {
    val indoor = NewSessionForm(method = RecordingMethod.AirBeamFixed)
    assertEquals("Indoor", EnStrings.summarize(indoor).placement)

    val outdoor = indoor.copy(indoor = false, outdoorLocation = GeoLocation(50.061234, 19.937891))
    assertEquals("Outdoor (50.0612, 19.9379)", EnStrings.summarize(outdoor).placement)
  }

  @Test
  fun `a mobile session has no placement row`() {
    val form = NewSessionForm(method = RecordingMethod.AirBeamMobile, indoor = false, outdoorLocation = GeoLocation(
      50.0,
      19.0
    )
    )

    assertNull(EnStrings.summarize(form).placement)
  }
}

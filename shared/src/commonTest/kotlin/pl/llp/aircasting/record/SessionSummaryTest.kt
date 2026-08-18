package pl.llp.aircasting.record

import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionSummaryTest {

  @Test
  fun `the summary is the form, phrased for the confirmation card`() {
    val form = NewSessionForm(
      method = RecordingMethod.PhoneMicrophone,
      name = "Morning commute",
      tagsInput = "school, traffic",
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
}

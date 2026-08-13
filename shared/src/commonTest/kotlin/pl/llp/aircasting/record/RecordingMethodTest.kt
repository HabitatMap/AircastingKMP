package pl.llp.aircasting.record

import pl.llp.aircasting.i18n.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingMethodTest {
  @Test
  fun the_order_is_the_one_the_design_shows() {
    assertEquals(
      listOf(
        RecordingMethod.AirBeamMobile,
        RecordingMethod.AirBeamFixed,
        RecordingMethod.PhoneMicrophone,
      ),
      RecordingMethod.entries,
    )
  }
  @Test
  fun every_locale_describes_every_method_distinctly() {
    AppStrings.forEach { (tag, strings) ->
      val copies = RecordingMethod.entries.map { strings.copyFor(it) }
      copies.forEach {
        assertTrue(
          it.tag.isNotBlank() && it.title.isNotBlank() && it.body.isNotBlank(),
          "blank method copy in $tag",
        )
      }
      assertEquals(copies.size, copies.toSet().size, "duplicate method copy in $tag")
    }
  }
}
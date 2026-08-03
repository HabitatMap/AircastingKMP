package pl.llp.aircasting.navigation

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsRouteTest {

  @Test
  fun `sections are in design order`() {
    // Figma 161:35267 → 161:35309, top → bottom on the Settings root (158:1048).
    // List order IS render order, so this is a design contract worth pinning — same
    // reasoning as AppTabTest for the bottom bar.
    assertEquals(
      listOf(
        SettingsRoute.Account,
        SettingsRoute.AirBeams,
        SettingsRoute.AppSettings,
        SettingsRoute.Help,
      ),
      SettingsRoute.sections,
    )
  }

  @Test
  fun `root is not a section`() {
    // Root *is* the list. If it leaked into sections it would render a row that navigates
    // to the screen you're already on.
    assertFalse(SettingsRoute.Root in SettingsRoute.sections)
  }

  @Test
  fun `every destination has a distinct non-blank title`() {
    // Every destination shows its title in the app bar, so a blank or duplicated one is a
    // user-visible bug that no compiler catches.
    val titles = (SettingsRoute.sections + SettingsRoute.Root).map { EnStrings.title(it) }
    assertTrue(titles.none { it.isBlank() }, "blank settings title in $titles")
    assertEquals(titles.size, titles.toSet().size, "duplicate settings title in $titles")
  }

  @Test
  fun `every section has a subtitle and root has none`() {
    // The subtitle is the supporting line on each root row. Root has no row, hence null —
    // this asserts the null is deliberate rather than a missed branch.
    SettingsRoute.sections.forEach { section ->
      val subtitle = EnStrings.subtitle(section)
      assertNotNull(subtitle, "missing subtitle for $section")
      assertTrue(subtitle.isNotBlank(), "blank subtitle for $section")
    }
    assertEquals(null, EnStrings.subtitle(SettingsRoute.Root))
  }
}

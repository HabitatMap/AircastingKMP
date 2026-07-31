package pl.llp.aircasting.navigation

import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTabTest {

  @Test
  fun `tabs are in design order`() {
    // Figma 131:10841 — Home, Explore, Record, Favorites, My data (left → right).
    // Enum order IS the bar order on both platforms, so it is a design contract worth pinning.
    assertEquals(
      listOf(AppTab.Home, AppTab.Explore, AppTab.Record, AppTab.Favorites, AppTab.MyData),
      AppTab.entries,
    )
  }

  @Test
  fun `every tab has a distinct non-blank label`() {
    val labels = AppTab.entries.map { EnStrings.label(it) }
    assertTrue(labels.none { it.isBlank() }, "blank tab label in $labels")
    assertEquals(labels.size, labels.toSet().size, "duplicate tab label in $labels")
  }
}
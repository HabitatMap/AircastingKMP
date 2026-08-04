package pl.llp.aircasting.settings

import pl.llp.aircasting.navigation.SettingsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettingsIconTest {

  @Test
  fun `every section has a distinct icon`() {
    val icons = SettingsRoute.sections.map { section ->
      assertNotNull(section.icon(), "missing icon for $section")
    }
    assertEquals(icons.size, icons.toSet().size, "duplicate settings icon in $icons")
  }

  @Test
  fun `root has no icon`() {
    assertEquals(null, SettingsRoute.Root.icon())
  }
}

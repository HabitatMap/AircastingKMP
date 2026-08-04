package pl.llp.aircasting.settings.account

import pl.llp.aircasting.i18n.EnStrings
import pl.llp.aircasting.navigation.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountActionTest {

  @Test
  fun `actions are in design order`() {
    assertEquals(
      listOf(
        AccountAction.ChangeEmail,
        AccountAction.ChangeUsername,
        AccountAction.ResetPassword,
      ),
      AccountAction.entries,
    )
  }
  @Test
  fun `every action has a distinct non-blank label`() {
    val labels = AccountAction.entries.map { EnStrings.label(it) }
    assertTrue(labels.none { it.isBlank() }, "blank account label in $labels")
    assertEquals(labels.size, labels.toSet().size, "duplicate account label in $labels")
  }

  @Test
  fun `section header is upper-case as designed`() {
    assertEquals("ACCOUNT", EnStrings.settingsAccountSectionHeader)
  }
  @Test
  fun `destructive and neutral button copy differ`() {
    assertTrue(EnStrings.accountSignOut.isNotBlank())
    assertTrue(EnStrings.accountDeleteAccount.isNotBlank())
    assertEquals(2, setOf(EnStrings.accountSignOut, EnStrings.accountDeleteAccount).size)
  }
}
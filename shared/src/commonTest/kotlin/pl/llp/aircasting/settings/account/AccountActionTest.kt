package pl.llp.aircasting.settings.account

import pl.llp.aircasting.i18n.EnStrings
import pl.llp.aircasting.navigation.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountActionTest {

  @Test
  fun `actions are in design order`() {
    // Figma 161:35968 → 181:12082 → 161:35979, top → bottom. The screen renders
    // AccountAction.entries directly, so declaration order IS render order — pin it.
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
    // Exhaustive `when` in label() catches a *missing* branch at compile time; it cannot
    // catch a copy-pasted branch that returns the wrong string. This can.
    val labels = AccountAction.entries.map { EnStrings.label(it) }
    assertTrue(labels.none { it.isBlank() }, "blank account label in $labels")
    assertEquals(labels.size, labels.toSet().size, "duplicate account label in $labels")
  }

  @Test
  fun `section header is upper-case as designed`() {
    // Figma 169:12805 ships the string already upper-cased — no textTransform in Compose,
    // so the casing lives in the copy. A sentence-case regression is invisible to the compiler.
    assertEquals("ACCOUNT", EnStrings.settingsAccountSectionHeader)
  }
  @Test
  fun `destructive and neutral button copy differ`() {
    assertTrue(EnStrings.accountSignOut.isNotBlank())
    assertTrue(EnStrings.accountDeleteAccount.isNotBlank())
    assertEquals(2, setOf(EnStrings.accountSignOut, EnStrings.accountDeleteAccount).size)
  }
}
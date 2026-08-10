package pl.llp.aircasting.data.auth

import app.cash.turbine.test
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredAuthSessionTest {

  @Test
  fun a_fresh_install_starts_signed_out() {
    assertEquals(AuthState.SignedOut, StoredAuthSession(MapSettings()).state.value)
  }

  @Test
  fun the_session_survives_a_process_restart() {
    val settings = MapSettings()
    StoredAuthSession(settings).start("tok-123")

    // A second instance over the same storage is what a cold app start looks like.
    assertEquals(AuthState.SignedIn("tok-123"), StoredAuthSession(settings).state.value)
  }

  @Test
  fun ending_the_session_erases_it_from_storage_too() {
    val settings = MapSettings()
    val session = StoredAuthSession(settings).apply { start("tok-123") }

    session.end()

    assertEquals(AuthState.SignedOut, session.state.value)
    // The important half: it must not come back on the next launch.
    assertEquals(AuthState.SignedOut, StoredAuthSession(settings).state.value)
  }

  @Test
  fun observers_see_every_transition() = runTest {
    val session = StoredAuthSession(MapSettings())

    session.state.test {
      assertEquals(AuthState.SignedOut, awaitItem())
      session.start("tok-123")
      assertEquals(AuthState.SignedIn("tok-123"), awaitItem())
      session.end()
      assertEquals(AuthState.SignedOut, awaitItem())
    }
  }
}

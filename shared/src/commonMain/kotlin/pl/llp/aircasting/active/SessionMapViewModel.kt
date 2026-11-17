package pl.llp.aircasting.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import pl.llp.aircasting.domain.TrackPoint

/**
 * One stream's track for our own mobile session, live from the DB. Null = still loading.
 * One instance per stream (keyed at the call site), like SessionGraphViewModel — so the inline
 * and full-screen maps share one subscription.
 */
class SessionMapViewModel(
  uuid: String,
  sensorName: String,
  repository: SessionDetailsRepository,
) : ViewModel() {
  val track: StateFlow<List<TrackPoint>?> = repository.observeTrack(uuid, sensorName)
    // A local read failing is unexpected; an empty map beats an error state with nothing to retry.
    .catch { e -> log.w(e) { "Session $uuid $sensorName track failed" }; emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

  private companion object {
    val log = Logger.withTag("SessionMap")
  }
}
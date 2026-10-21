package pl.llp.aircasting.recording

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import pl.llp.aircasting.domain.GeoLocation

/** Continuous fixes, kept up in the background (needs the `location` UIBackgroundMode).
 * Two iOS-specific points:
 * - flowOn(Main) is required. RecordingEngine.run() runs on Dispatchers.Default. A CLLocationManager created on a worker thread has no run loop, so its delegate is never called and nothing errors. flowOn moves only the code upstream of it, meaning the callbackFlow body, onto the main thread.
 * - The weak delegate. This is the same issue IosLocationProvider handles with a field. Here, mentioning delegate inside awaitClose keeps it alive in the coroutine's state.
 * */
@OptIn(ExperimentalForeignApi::class)
fun iosLocationFixes(): Flow<GeoLocation> = callbackFlow {
  val manager = CLLocationManager()
  val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
      (didUpdateLocations.lastOrNull() as? CLLocation)?.coordinate?.useContents {
        trySend(GeoLocation(latitude, longitude))
      }
    }
  }
  manager.delegate = delegate
  manager.desiredAccuracy = kCLLocationAccuracyBest
  manager.allowsBackgroundLocationUpdates = true
  manager.pausesLocationUpdatesAutomatically = false
  manager.showsBackgroundLocationIndicator = true
  manager.startUpdatingLocation()
  awaitClose {
    manager.stopUpdatingLocation()
    // Also keeps `delegate` reachable until here: CLLocationManager holds its delegate weakly.
    if (manager.delegate === delegate) manager.delegate = null
  }
}.flowOn(Dispatchers.Main) // the manager calls back on the run loop of the thread that created it
package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.domain.GeoLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider(
  private val manager: CLLocationManager = CLLocationManager(),
) : LocationProvider {

  // Strong ref: CLLocationManager.delegate is `weak`; without this the delegate
  // deallocates before the callback fires.
  private var activeDelegate: CLLocationManagerDelegateProtocol? = null

  override suspend fun current(): GeoLocation? {
    if (!authorized()) return null

    return suspendCancellableCoroutine { cont ->
      val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
          val fix = didUpdateLocations.lastOrNull() as? CLLocation
          finish(cont, fix?.geo())
        }
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
          finish(cont, null)
        }
      }
      activeDelegate = delegate
      manager.delegate = delegate
      cont.invokeOnCancellation { clear() }
      manager.requestLocation()
    }
  }
  private fun finish(cont: kotlinx.coroutines.CancellableContinuation<GeoLocation?>, value: GeoLocation?) {
    if (cont.isActive) cont.resume(value)
    clear()
  }

  private fun clear() {
    manager.delegate = null
    activeDelegate = null
  }

  private fun authorized(): Boolean {
    val status = manager.authorizationStatus
    return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
      status == kCLAuthorizationStatusAuthorizedAlways
  }
  private fun CLLocation.geo(): GeoLocation =
    coordinate.useContents { GeoLocation(latitude, longitude) }
}

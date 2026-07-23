package com.lunarlogic.aircasting.home

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined

class IosLocationPermission(
  private val manager: CLLocationManager = CLLocationManager(),
) {
  fun request() {
    if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
      manager.requestWhenInUseAuthorization()
    }
  }
}
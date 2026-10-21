package pl.llp.aircasting.recording

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.llp.aircasting.domain.GeoLocation

/** 1 s high-accuracy fixes, as legacy `LocationHelper.createLocationRequest`. Ends at once without permission. */
@SuppressLint("MissingPermission") // checked below
fun androidLocationFixes(context: Context): Flow<GeoLocation> = callbackFlow {
  if (!hasLocationPermission(context)) {
    Logger.withTag("PhoneLocation").w { "No location permission; measurements fall back to the last fix" }
    close()
    return@callbackFlow
  }
  val client = LocationServices.getFusedLocationProviderClient(context)
  val callback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      result.lastLocation?.let { trySend(GeoLocation(it.latitude, it.longitude)) }
    }
  }
  client.requestLocationUpdates(
    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000).build(),
    callback,
    Looper.getMainLooper(),
  )
  awaitClose { client.removeLocationUpdates(callback) }
}

private fun hasLocationPermission(context: Context) =
  listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
  }
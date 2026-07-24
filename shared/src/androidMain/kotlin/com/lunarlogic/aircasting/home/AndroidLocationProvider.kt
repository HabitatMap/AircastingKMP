package com.lunarlogic.aircasting.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.lunarlogic.aircasting.domain.GeoLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(context: Context) : LocationProvider {
  private val appContext = context.applicationContext
  private val client = LocationServices.getFusedLocationProviderClient(appContext)

  override suspend fun current(): GeoLocation? {
    val hasPerm = hasLocationPermission()
    println("AIRDIAG/LOC: hasLocationPermission = $hasPerm")
    if (!hasPerm) return null
    val result = runCatching { lastKnown() ?: requestFresh() }
      .onFailure { println("AIRDIAG/LOC: location lookup threw ${it::class.simpleName}: ${it.message}") }
      .getOrNull()
    println("AIRDIAG/LOC: result = $result")
    return result
  }

  @Suppress("MissingPermission") // guarded by hasLocationPermission()
  private suspend fun requestFresh(): GeoLocation? {
    val cts = CancellationTokenSource()
    val request = CurrentLocationRequest.Builder()
      .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
      .setDurationMillis(10_000)
      .build()
    return suspendCancellableCoroutine { cont ->
      client.getCurrentLocation(request, cts.token)
        .addOnSuccessListener { loc -> cont.resume(loc?.let { GeoLocation(it.latitude, it.longitude) }) }
        .addOnFailureListener { cont.resume(null) }
      cont.invokeOnCancellation { cts.cancel() }
    }
  }

  @Suppress("MissingPermission")
  private suspend fun lastKnown(): GeoLocation? =
    suspendCancellableCoroutine { cont ->
      client.lastLocation
        .addOnSuccessListener { loc -> cont.resume(loc?.let { GeoLocation(it.latitude, it.longitude) }) }
        .addOnFailureListener { cont.resume(null) }
    }

  private fun hasLocationPermission(): Boolean {
    fun granted(p: String) =
      ContextCompat.checkSelfPermission(appContext, p) == PackageManager.PERMISSION_GRANTED
    return granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
      granted(Manifest.permission.ACCESS_COARSE_LOCATION)
  }
}
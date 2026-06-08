package com.edistrive.aura.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

object LocationProvider {

    fun isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(context: Context): Location? {
        if (!isPermissionGranted(context)) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try to get location with a 10-second timeout
        return try {
            withTimeout(10_000) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    getLocationModern(lm, context)
                } else {
                    getLocationLegacy(lm)
                }
            }
        } catch (_: TimeoutCancellationException) {
            // Timeout: fall back to last known location
            getLastKnownLocation(lm)
        } catch (_: Exception) {
            getLastKnownLocation(lm)
        }
    }

    private suspend fun getLocationModern(lm: LocationManager, context: Context): Location? {
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        )

        for (provider in providers) {
            // Skip providers that don't exist on this device (e.g. FUSED on non-GMS phones)
            if (!lm.allProviders.contains(provider)) continue
            if (!lm.isProviderEnabled(provider)) continue

            try {
                val result = suspendCancellableCoroutine<Location?> { cont ->
                    var resumed = false
                    try {
                        lm.getCurrentLocation(
                            provider,
                            null,
                            context.mainExecutor
                        ) { loc ->
                            if (!resumed) {
                                resumed = true
                                cont.resume(loc)
                            }
                        }
                    } catch (e: Exception) {
                        if (!resumed) {
                            resumed = true
                            cont.resume(null)
                        }
                    }
                }
                if (result != null) return result
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private suspend fun getLocationLegacy(lm: LocationManager): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        return suspendCancellableCoroutine { cont ->
            var resolved = false

            for (provider in providers) {
                if (!lm.isProviderEnabled(provider)) continue
                try {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(
                        provider,
                        object : LocationListener {
                            override fun onLocationChanged(loc: Location) {
                                if (!resolved) {
                                    resolved = true
                                    cont.resume(loc)
                                }
                            }
                            override fun onProviderDisabled(p: String) {}
                            override fun onProviderEnabled(p: String) {}
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(p: String, s: Int, e: Bundle) {}
                        },
                        Looper.getMainLooper()
                    )
                } catch (_: SecurityException) {}
            }

            // Try last known location immediately
            val lastKnown = getLastKnownLocation(lm)
            if (lastKnown != null && !resolved) {
                resolved = true
                cont.resume(lastKnown)
                return@suspendCancellableCoroutine
            }

            // Timeout after 8 seconds
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!resolved) {
                    resolved = true
                    cont.resume(null)
                }
            }, 8_000)
        }
    }

    private fun getLastKnownLocation(lm: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return loc
            } catch (_: SecurityException) {}
        }
        return null
    }
}

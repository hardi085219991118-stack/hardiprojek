package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GpsLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GpsLocationResult? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location: Location? = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (location != null) {
                GpsLocationResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
            } else {
                getLocationFromManagerFallback(context)
            }
        } catch (e: Exception) {
            getLocationFromManagerFallback(context)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocationFromManagerFallback(context: Context): GpsLocationResult? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            bestLocation?.let {
                GpsLocationResult(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    timestamp = it.time
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "Kandang Broiler") {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to web browser Google Maps
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka peta: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

package fr.geming400.localisationhelper.actions

import android.annotation.SuppressLint
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationRequestCompat
import fr.geming400.localisationhelper.utils.SimpleLocation
import fr.geming400.localisationhelper.utils.Utils
import java.util.concurrent.CompletableFuture

/**
 * Serialization format: `latitude;longitude`
 */
class LocationGetterAction(name: String) : Action<SimpleLocation>(name) {
    @Throws(MalformedRawActionException::class)
    override fun parse(rawContent: String): SimpleLocation {
        val geolocation: Array<String?> =
            rawContent.split(";")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

        if (geolocation.size == 2) {
            try {
                val latitude = Math.clamp(geolocation[0]!!.toDouble(), -90.0, 90.0)
                val longitude = Math.clamp(geolocation[0]!!.toDouble(), -180.0, 180.0)

                return SimpleLocation(latitude, longitude)
            } catch (e: NumberFormatException) {
                throw MalformedRawActionException(
                    "Couldn't parse latitude/longitude doubles",
                    e,
                    this,
                    rawContent
                )
            }
        } else {
            throw MalformedRawActionException(
                "Geolocation data doesn't contain 2 entries",
                this,
                rawContent
            )
        }
    }

    override fun serializeResult(location: SimpleLocation): String {
        return String.format("%s;%s", location.latitude, location.longitude)
    }

    @SuppressLint("MissingPermission")
    override fun execute(context: Context): CompletableFuture<SimpleLocation?>? {
        val locationManager =
            context.getSystemService(LocationManager::class.java)

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val completableFuture = CompletableFuture<SimpleLocation?>()

        if (hasGps && Utils.hasLocationPermissions(context)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LocationRequestCompat.PASSIVE_INTERVAL,
                Int.MAX_VALUE.toFloat()
            ) { location: Location? ->
                completableFuture.complete(
                    SimpleLocation.ofLocation(
                        location
                    )
                )
            }

            return completableFuture
        }

        return null
    }

    // TODO:
    override fun onReceive(context: Context, sender: String, pendingResult: PendingResult, stage: Stage, rawContent: String) {
        if (stage == Stage.RECEIVE_OTHER_PHONE) {

        } else {

        }
    }
}
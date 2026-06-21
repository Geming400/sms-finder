package fr.geming400.localisationhelper.actions

import android.annotation.SuppressLint
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationRequestCompat
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.SerializableGeolocation
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.utils.SimpleLocation
import fr.geming400.localisationhelper.utils.Timestamp
import fr.geming400.localisationhelper.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * Serialization format: `latitude;longitude`
 */
class LocationGetterAction(name: String) : Action<SimpleLocation>(name) {
    @Throws(MalformedRawActionException::class)
    override fun parse(rawContent: String): SimpleLocation {
        val geolocation = rawContent.split(";")

        if (geolocation.size == 2) {
            try {
                val latitude = Math.clamp(geolocation[0].toDouble(), -90.0, 90.0)
                val longitude = Math.clamp(geolocation[1].toDouble(), -180.0, 180.0)

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
            ) { location ->
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

    override fun onReceive(context: Context, sender: String, pendingResult: PendingResult, stage: Stage, trackingData: TrackingData, rawContent: String) {
        val jsonDataStore = JsonDataStore(context)
        if (stage == Stage.RECEIVE_HOST) {
            val asContact = trackingData.getContact(context)

            val geolocation = parse(rawContent)

            CoroutineScope(Dispatchers.IO.limitedParallelism(1, "LocationGetterAction's onReceive (Stage.RECEIVE_HOST)")).launch {
                jsonDataStore.updateTrackedContact(asContact) {
                    it.copy(
                        geolocation = SerializableGeolocation(
                            geolocation.latitude,
                            geolocation.longitude,
                            Timestamp.now()
                        )
                    )
                }
            }
        } else {
            this.sendDataSMS(context, sender, trackingData.privateKey!!)
        }
    }
}
package fr.geming400.localisationhelper.utils

import android.location.Location
import androidx.annotation.FloatRange
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

@Serializable
data class SimpleLocation(
    @field:FloatRange(from = -90.0, to = 90.0) val latitude: Double,
    @field:FloatRange(from = -180.0, to = 180.0) val longitude: Double,
    @ApiStatus.AvailableSince("1.0.4") val accuracy: Float = -1f
) {
    fun hasAccuracy() = this.accuracy != -1f

    fun toFormattedString(): String =
        "$latitude $longitude"

    companion object {
        fun ofLocation(location: Location) =
            SimpleLocation(
                location.latitude,
                location.longitude,
                if (location.hasAccuracy())
                    location.accuracy
                else
                    -1f
            )
    }
}

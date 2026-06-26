package fr.geming400.localisationhelper.utils

import android.location.Location
import androidx.annotation.FloatRange
import kotlinx.serialization.Serializable

@Serializable
data class SimpleLocation(
    @field:FloatRange(from = -90.0, to = 90.0) val latitude: Double,
    @field:FloatRange(from = -180.0, to = 180.0) val longitude: Double
) {
    fun toFormattedString(): String =
        "$latitude $longitude"

    companion object {
        fun ofLocation(location: Location): SimpleLocation {
            return SimpleLocation(location.latitude, location.longitude)
        }
    }
}

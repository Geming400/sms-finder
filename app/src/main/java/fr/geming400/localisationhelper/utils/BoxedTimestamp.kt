package fr.geming400.localisationhelper.utils

import kotlinx.serialization.Serializable

@Serializable
data class BoxedTimestamp<out T>(val timestamp: Timestamp, val value: T) {
    fun getRelativeTime() =
        this.timestamp.getRelativeTime()

    companion object {
        fun <T> now(value: T): BoxedTimestamp<T> {
            return BoxedTimestamp(Timestamp.now(), value)
        }
    }
}

package fr.geming400.localisationhelper.utils

import androidx.annotation.NonNull
import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
data class BoxedTimestamp<out T>(val timestamp: Timestamp, val value: T) {
    fun getRelativeTime() =
        this.timestamp.getRelativeTime()

    @NonNull
    fun getOrThrow(): T
        = Objects.requireNonNull(this.value, "Tried unboxing value of boxed timestamp $this but failed because value was null !")

    companion object {
        fun <T> now(value: T): BoxedTimestamp<T> {
            return BoxedTimestamp(Timestamp.now(), value)
        }

        fun <T> nowNull(): BoxedTimestamp<T?> {
            return BoxedTimestamp(Timestamp.now(), null)
        }
    }
}

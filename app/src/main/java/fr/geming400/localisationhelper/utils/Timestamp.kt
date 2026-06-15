package fr.geming400.localisationhelper.utils

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Serializable
data class Timestamp(
    @field:IntRange(from = 0) val timestamp: Long,
    val offsetID: String
) {
    fun getZoneOffset(): ZoneOffset {
        return ZoneOffset.of(offsetID)
    }

    fun toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofEpochSecond(timestamp, 0, getZoneOffset())
    }

    override fun toString(): String {
        return "Location(dateTime = ${toLocalDateTime()})"
    }

    companion object {
        fun now(): Timestamp {
            return Timestamp(Utils.getCurrentEpoch(), OffsetDateTime.now().offset.id)
        }
    }
}

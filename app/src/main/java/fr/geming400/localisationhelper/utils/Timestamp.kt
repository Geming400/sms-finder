package fr.geming400.localisationhelper.utils

import android.text.format.DateUtils
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

    fun getRelativeTime(): String =
        DateUtils.getRelativeTimeSpanString(
            this.toLocalDateTime().toInstant(this.getZoneOffset()).toEpochMilli(),
            Utils.getCurrentEpochMs(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()


    override fun toString(): String {
        return "Timestamp(dateTime = ${toLocalDateTime()})"
    }

    companion object {
        fun now(): Timestamp {
            return Timestamp(Utils.getCurrentEpoch(), OffsetDateTime.now().offset.id)
        }
    }
}

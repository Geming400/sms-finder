package fr.geming400.localisationhelper.datastore

import android.content.Context
import androidx.annotation.IntRange
import contacts.core.Contacts
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import kotlinx.serialization.Serializable

@Serializable
data class TrackingData(
    // Contact info
    val lookupKey: String,
    val contactID: Long,
    val linkedPhoneNumber: String? = null,

    // Tracking info
    val geolocation: SerializableGeolocation? = null
) {
    val lookupKeyWithId: LookupQuery.LookupKeyWithId
        get() = LookupQuery.LookupKeyWithId(lookupKey, contactID)

    fun getContact(context: Context): Contact = Contacts(context)
        .lookupQuery()
        .whereLookupKeyWithIdMatches(LookupQuery.LookupKeyWithId(lookupKey, contactID))
        .find()
        .first()

    fun areContactInfoEqual(contact: Contact) =
        contact.lookupKey == lookupKey && contact.id == contactID
}

@Serializable
data class SerializableGeolocation(
    val longitude: Double,
    val latitude: Double,
    /**
     * When this geolocation data got recorded. If left to `0` then the date is unknown.
     *
     * Value is a timestamp
     */
    @field:IntRange(from = 0)
    val lastTimeRecorded: Long = 0
)
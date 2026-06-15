package fr.geming400.localisationhelper.datastore

import android.content.Context
import contacts.core.Contacts
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.utils.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class TrackingData(
    // Contact info
    val lookupKey: String,
    val linkedPhoneNumber: String? = null,

    // Tracking info
    val geolocation: SerializableGeolocation? = null,
    val lastPingAnswer: Timestamp? = null
) {
    val lookupKeyWithId: LookupQuery.LookupKeyWithId
        get() = LookupQuery.LookupKeyWithId(lookupKey, 0)

    fun getContact(context: Context): Contact = Contacts(context)
        .lookupQuery()
        .whereLookupKeyWithIdMatches(LookupQuery.LookupKeyWithId(lookupKey, 0))
        .find()
        .first()

    fun areContactInfoEqual(contact: Contact) =
        contact.lookupKey == lookupKey
}

@Serializable
data class SerializableGeolocation(
    val longitude: Double,
    val latitude: Double,
    /**
     * When this geolocation data got recorded
     */
    val lastTimeRecorded: Timestamp? = null
)
package fr.geming400.localisationhelper.datastore

import android.content.Context
import contacts.core.Contacts
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.utils.BoxedTimestamp
import fr.geming400.localisationhelper.utils.SimpleLocation
import fr.geming400.localisationhelper.utils.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class TrackingData(
    // Contact info
    val lookupKey: String,
    val linkedPhoneNumber: String? = null,
    val privateKey: String? = null,
    // I couldn't think of a better name :(
    val hasSeenShouldGetAddedOnBothDevicesDisclaimer: Boolean = false,

    // Tracking info
    val geolocation: BoxedTimestamp<SimpleLocation>? = null,
    val lastPingAnswer: Timestamp? = null,
    val lastRecordedBatteryCharge: BoxedTimestamp<Float>? = null,
    val extraInfo: BoxedTimestamp<ExtraInfo>? = null
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
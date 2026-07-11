package fr.geming400.localisationhelper.datastore

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.LogTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.internal.toImmutableList
import java.io.InputStream
import java.io.OutputStream

class JsonDataStore(private val context: Context) {
    fun trackedContactsFlow(): Flow<List<TrackingData>> = context.dataStore.data.map { savedData ->
        savedData.trackedContacts
    }

    suspend fun addTrackedContact(contact: Contact) {
        addTrackedContact(LookupQuery.LookupKeyWithId(contact.lookupKey!!, contact.id))
    }

    suspend fun addTrackedContact(lookupKeyWithId: LookupQuery.LookupKeyWithId) {
        context.dataStore.updateData { savedData ->
            savedData.copy(
                trackedContacts = savedData.trackedContacts.plus(
                    TrackingData(lookupKeyWithId.lookupKey)
                )
            )
        }
    }

    suspend fun updateTrackedContact(contact: Contact, trackingDataChanger: (oldData: TrackingData) -> TrackingData) {
        context.dataStore.updateData { savedData ->
            val newTrackedContacts = mutableListOf<TrackingData>()

            // We replace the old trackingData with the new one
            savedData.trackedContacts.forEach {
                if (it.areContactInfoEqual(contact)) {
                    newTrackedContacts.add(trackingDataChanger(it))
                } else {
                    newTrackedContacts.add(it)
                }
            }

            savedData.copy(
                trackedContacts = newTrackedContacts
            )
        }
    }

    /**
     * @return if the operation succeeded
     */
    suspend fun removeTrackedContact(contact: Contact): Boolean {
        Log.d(LogTags.DATA_STORE, "Deleting contact $contact")

        var res = false

        context.dataStore.updateData { savedData ->
            savedData.copy(
                trackedContacts = savedData.trackedContacts.filter {
                    res = it.areContactInfoEqual(contact)

                    // If the contact infos are equal then we can
                    // remove this tracked contact
                    return@filter !res
                }
            )
        }

        return res
    }

    fun getTrackedContact(contacts: Collection<TrackingData>, contact: Contact): TrackingData {
        val trackingData = getTrackedContactOrNull(contacts, contact)
            ?: throw NoSuchElementException("No elements correspond to '$contact' in '$contacts'")

        return trackingData
    }

    fun getTrackedContactOrNull(contacts: Collection<TrackingData>, contact: Contact): TrackingData? {
        contacts.forEach {
            if (it.areContactInfoEqual(contact))
                return it
        }

        return null
    }

    fun getFirstTrackedContact(contacts: Collection<TrackingData>, phoneNumber: String?): TrackingData? {
        if (phoneNumber == null)
            return null

        contacts.forEach {
            if (it.linkedPhoneNumber == phoneNumber)
                return@getFirstTrackedContact it
        }

        return null
    }

    suspend fun addRecentlyAccessedContact(contact: Contact, recentlyAccessedContacts: List<String>) {
        if (recentlyAccessedContacts.contains(contact.lookupKey)) {
            val list = recentlyAccessedContacts.toMutableList()
            list.remove(contact.lookupKey!!)
            list.add(0, contact.lookupKey!!)

            context.dataStore.updateData {
                it.copy(
                    lastAccessedContacts = list
                )
            }
        } else {
            val list = recentlyAccessedContacts.toMutableList()
            list.add(0, contact.lookupKey!!)

            val finalList =
                list.subList(
                    0,
                    list.size.coerceAtMost(LocalisationHelperData.MAX_RECENTLY_ACCESSED_CONTACTS.toInt())
                ).toImmutableList()

            context.dataStore.updateData {
                it.copy(
                    lastAccessedContacts = finalList
                )
            }
        }
    }
}

val Context.dataStore: DataStore<LocalisationHelperData> by dataStore(
    fileName = "saved.json",
    serializer = LocalisationHelperDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { LocalisationHelperDataSerializer.defaultValue }
)

@Serializable
data class LocalisationHelperData(
    val trackedContacts: List<TrackingData> = arrayListOf(),
    val passwordHash: String? = null,
    val sawXiaomiNotice: Boolean = Build.MANUFACTURER != "Xiaomi",
    val lastAccessedContacts: List<String> = arrayListOf(),
    val firstTimeOpening: Boolean = true
) {
    companion object {
        const val MAX_RECENTLY_ACCESSED_CONTACTS: Short = 3
    }
}

object LocalisationHelperDataSerializer : Serializer<LocalisationHelperData> {
    override val defaultValue: LocalisationHelperData = LocalisationHelperData()

    override suspend fun readFrom(input: InputStream): LocalisationHelperData =
        try {
            Json.decodeFromString<LocalisationHelperData>(
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read LocalisationHelperData (${e.message}", e)
        }

    override suspend fun writeTo(
        t: LocalisationHelperData,
        output: OutputStream
    ) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(t)
                    .encodeToByteArray()
            )
        }
    }
}
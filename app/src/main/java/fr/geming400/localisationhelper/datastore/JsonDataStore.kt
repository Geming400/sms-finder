package fr.geming400.localisationhelper.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class JsonDataStore(val context: Context) {
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
                    TrackingData(lookupKeyWithId.lookupKey, lookupKeyWithId.contactId)
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

    suspend fun removeTrackedContact(contact: Contact): Boolean {
        var res = false

        context.dataStore.updateData { savedData ->
            savedData.copy(
                trackedContacts = savedData.trackedContacts.dropWhile {
                    res = it.areContactInfoEqual(contact)
                    return@dropWhile res
                }
            )
        }

        return res
    }

    fun getTrackedContact(contacts: Collection<TrackingData>, contact: Contact): TrackingData {
        contacts.forEach {
            if (it.areContactInfoEqual(contact))
                return@getTrackedContact it
        }

        throw NoSuchElementException("No elements correspond to '$contact' in '$contacts'")
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
    val passwordHash: String? = null
)

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
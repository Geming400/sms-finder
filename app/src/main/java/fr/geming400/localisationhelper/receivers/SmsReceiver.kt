package fr.geming400.localisationhelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.actions.Actions
import fr.geming400.localisationhelper.actions.BaseAction
import fr.geming400.localisationhelper.actions.PayloadType
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Base64


class SmsReceiver : BroadcastReceiver() {
    private fun getContactInfo(context: Context, sender: String): ContactInfo =
        runBlocking {
            val jsonDataStore = JsonDataStore(context)

            val appData = context.dataStore.data.first()
            val trackedContacts = jsonDataStore.trackedContactsFlow().first()

            val trackingData = jsonDataStore.getFirstTrackedContact(trackedContacts, sender)!!
            val privateKeys = PrivateKeys(trackingData.privateKey!!, appData.passwordHash!!)

            ContactInfo(trackingData, privateKeys)
        }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            if (bundle != null) {
                // 1. Parsing the incoming message
                val pdus = bundle.get("pdus") as Array<*>

                var body = ""
                var sender = ""

                // Constructing the final message
                // by appending all sub-messages
                for (pdu in pdus) {
                    val message =
                        SmsMessage.createFromPdu(pdu as ByteArray?, bundle.getString("format"))
                    sender = message.originatingAddress!!
                    body = body.plus(message.messageBody)
                }

                if (body.contains("--ignore")) {
                    Log.i(LogTags.SMS_RECEIVER, "Sms received with flag '--ignore', ignoring")
                    return
                }


                // 2. We get the payload type and separate the headers
                val headersAndEncryptedContent = body.split(":", limit = 2).toMutableList()
                var headers = headersAndEncryptedContent[0].split("/", limit = 2)
                val payloadType = PayloadType.fromPayloadName(headers[0])!!

                val contactInfo = this.getContactInfo(context, sender)
                val privateKey = contactInfo.privateKeys.getRightPrivateKey(payloadType)

                // 3. We decrypt the encrypted content
                // We copy the headersAndEncryptedContent list
                val headersAndRawContent = arrayListOf<String>().plus(headersAndEncryptedContent).toMutableList()

                // headersAndEncryptedContent.size == 1: There's no content (the name is still encrypted), so no need to decode it
                // headersAndEncryptedContent.size == 2: There's content (the name is still encrypted)
                if (headersAndEncryptedContent.size == 2) {
                    headersAndRawContent[1] = String(Utils.cyclicXor(
                        Base64.getDecoder().decode(headersAndEncryptedContent[1].toByteArray()),
                        privateKey.toByteArray()
                    ))
                } else if (headersAndEncryptedContent.size != 1) {
                    Log.w(
                        LogTags.SMS_RECEIVER,
                        "Splitted received (encrypted) body but was of invalid size (got ${headersAndEncryptedContent.size} instead). Either the body is malformed or the sms not for us"
                    )

                    return
                }

                headers = headersAndRawContent[0].split("/", limit = 2)
                Log.d(LogTags.SMS_RECEIVER, "headersAndEncryptedContent = $headersAndEncryptedContent")
                Log.d(LogTags.SMS_RECEIVER, "headersAndRawContent = $headersAndRawContent")
                Log.d(LogTags.SMS_RECEIVER, "headers = $headers")
                if (headers.size == 2) {
                    // The action's name is also encrypted
                    val name = String(
                        Utils.cyclicXor(
                            Base64.getDecoder().decode(headers[1].toByteArray()),
                            privateKey.toByteArray()
                        )
                    )

                    Log.i(
                        LogTags.SMS_RECEIVER,
                        "Received $payloadType payload of action $name !"
                    )

                    val rawContent =
                        if (headersAndRawContent.size == 2) headersAndRawContent[1] else ""

                    val pendingResult = goAsync()
                    try {
                        Actions.getByNameTypeless(name)?.onReceive(
                            context,
                            sender,
                            pendingResult,
                            BaseAction.Stage.fromPayloadType(payloadType),
                            contactInfo.trackingData,
                            rawContent
                        )
                    } finally {
                        pendingResult.finish()
                    }
                } else {
                    Log.w(
                        LogTags.SMS_RECEIVER,
                        "Splitted received body but was of invalid size (got ${headers.size} instead). Either the body is malformed or the sms not for us"
                    )
                }
            }
        }
    }

    data class PrivateKeys(val trackedContactKey: String?, val thisPhoneKey: String?) {
        /**
         * Gets the right private key according to the payload type
         * @param payloadType the payload type
         * @return the right private key
         * @throws NullPointerException if the returned private key is null
         */
        fun getRightPrivateKey(payloadType: PayloadType): String {
            return if (payloadType == PayloadType.INSTRUCTION) {
                this.trackedContactKey!!
            } else {
                this.thisPhoneKey!!
            }
        }
    }

    data class ContactInfo(val trackingData: TrackingData, val privateKeys: PrivateKeys)
}
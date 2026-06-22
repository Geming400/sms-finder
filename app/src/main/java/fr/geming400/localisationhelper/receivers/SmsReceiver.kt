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



                // 2. We get the payload type

                // The payload type is always unencrypted
                val firstPart = body.split("/")[0]
                val payloadType = PayloadType.fromPayloadName(firstPart)!!

                val contactInfo = this.getContactInfo(context, sender)
                val privateKey = contactInfo.privateKeys.getRightPrivateKey(payloadType)



                // 3. We unencrypt the message

                // This second part is in the format "actionName:payloadData"
                // if the payload type is DATA.
                // However, if it's INSTRUCTION, then it's "actionName"
                val secondPart = body.split("/", limit = 2)[1]
                val actionNameAndContent = String(Utils.cyclicXor(
                    Base64.getDecoder().decode(secondPart.toByteArray()),
                    privateKey.toByteArray()
                )).split(":", limit = 2)

                val actionName = actionNameAndContent[0]
                val actionDataPayload = actionNameAndContent.getOrNull(1)

                if (body.split("/", limit = 2).size == 2) {
                    Log.i(
                        LogTags.SMS_RECEIVER,
                        "Received $payloadType payload of action $actionName !"
                    )

                    val pendingResult = goAsync()
                    try {
                        Actions.getByNameTypeless(actionName)?.onReceive(
                            context,
                            sender,
                            pendingResult,
                            BaseAction.Stage.fromPayloadType(payloadType),
                            contactInfo.trackingData,
                            actionDataPayload ?: ""
                        )
                    } finally {
                        pendingResult.finish()
                    }
                } else {
                    Log.w(
                        LogTags.SMS_RECEIVER,
                        "Splitted received body but was of invalid size (got ${actionNameAndContent.size} instead). Either the body is malformed or the sms not for us"
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
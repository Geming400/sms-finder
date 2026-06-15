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


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            if (bundle != null) {
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


                val headersAndRawContent = body.split(":", limit = 2)
                val headers = headersAndRawContent[0].split("/", limit = 2)
                if (headers.size == 2) {
                    val payloadType = PayloadType.fromPayloadName(headers[0])!!
                    val name = headers[1]

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
}
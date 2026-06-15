package fr.geming400.localisationhelper.receivers

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import androidx.annotation.RequiresPermission
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.actions.Actions
import fr.geming400.localisationhelper.actions.BaseAction
import fr.geming400.localisationhelper.actions.PayloadType


class SmsReceiver : BroadcastReceiver() {
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>

                var body = ""
                var sender = ""

                // Constructing the final message
                // by appending all sub-messages
                for (pdu in pdus) {
                    val message: SmsMessage = SmsMessage.createFromPdu(pdu as ByteArray?, bundle.getString("format"))
                    sender = message.originatingAddress!!
                    body = body.plus(message.messageBody)
                }

//                Log.i(LogTags.SMS_RECEIVER, "Sms received: sent by %s:".format(sender))
//                Log.i(LogTags.SMS_RECEIVER, body)

                if (body == "a")
                    queryLocation(context, sender)

                val headersAndRawContent = body.split(":", limit = 2)
                val headers = headersAndRawContent[0].split("/", limit = 2)
                if (headers.size == 2) {
                    val payloadType = PayloadType.fromPayloadName(headers[0])!!
                    val name = headers[1]

                    Log.i(LogTags.SMS_RECEIVER, "Received $payloadType payload of action $name !")

                    val rawContent = if (headersAndRawContent.size == 2) headersAndRawContent[1] else ""

                    val pendingResult = goAsync()
                    try {
                        Actions.getByNameTypeless(name)?.onReceive(context, sender, pendingResult, BaseAction.Stage.fromPayloadType(payloadType), rawContent)
                    } finally {
                        pendingResult.finish()
                    }
                } else {
                    Log.w(LogTags.SMS_RECEIVER,"Splitted received body but was of invalid size (got ${headers.size} instead). Either the body is malformed or the sms not for us")
                }
            }
        }
    }

    private fun sendMessage(context: Context, sender: String?, body: String) {
        Log.i(LogTags.SMS_RECEIVER, "Sending sms to $sender with body: $body")

//        val sentIntent = PendingIntent.getBroadcast(
//            context,
//            0,
//            Intent("SMS_SENT"),
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )

//        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
//            override fun onReceive(ctx: Context, intent: Intent) {
//                when (resultCode) {
//                    Activity.RESULT_OK -> {
//                        Log.i(LogTags.SMS_RECEIVER, "SMS sent successfully")
//                    }
//
//                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
//                        Log.e(LogTags.SMS_RECEIVER, "Generic failure")
//                    }
//
//                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
//                        Log.e(LogTags.SMS_RECEIVER, "No service")
//                    }
//
//                    SmsManager.RESULT_ERROR_NULL_PDU -> {
//                        Log.e(LogTags.SMS_RECEIVER, "Null PDU")
//                    }
//
//                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
//                        Log.e(LogTags.SMS_RECEIVER, "Radio off")
//                    }
//
//                    else -> {
//                        Log.e(LogTags.SMS_RECEIVER, "Got other error code: $resultCode. Result data is $resultData")
//                    }
//                }
//            }
//        }, IntentFilter("SMS_SENT"), ContextCompat.RECEIVER_NOT_EXPORTED)

        val smsManager = context.getSystemService(SmsManager::class.java)
//        smsManager.sendMultipartTextMessage(sender, null, smsManager.divideMessage(body), arrayListOf(sentIntent), null)
        smsManager.sendMultipartTextMessage(sender, null, smsManager.divideMessage(body), null, null)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun queryLocation(context: Context, sender: String?) {
        val locationFuture = Actions.LOCATION.execute(context)

        val pendingResult = goAsync()
        if (locationFuture == null) {
            sendMessage(context, sender, "My last known location is unknown :(")
        } else {
            locationFuture.whenComplete { location, throwable ->
                try {
                    val text = if (throwable != null) {
                        "My last known location is unknown :("
                    } else {
                        "My last known location is %s° latitude and %s° longitude"
                            .format(location?.latitude, location?.longitude)
                    }

                    sendMessage(context, sender, text)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
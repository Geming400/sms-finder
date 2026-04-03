package fr.geming400.localisationhelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import fr.geming400.localisationhelper.LogTags


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LogTags.SMS_RECEIVER, "Received broadcast %s (with context %s) !".format(intent, context))
        if (intent.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>

                var body = ""
                var sender: String? = ""

                // Constructing the final message
                // by appending all sub messages
                for (pdu in pdus) {
                    val message: SmsMessage = SmsMessage.createFromPdu(pdu as ByteArray?, bundle.getString("format"))
                    sender = message.originatingAddress
                    body = body.plus(message.messageBody)
                }

                Log.i(LogTags.SMS_RECEIVER, "Sms received: sent by %s:".format(sender))
                Log.i(LogTags.SMS_RECEIVER, body)

                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(sender, null, "Hi", null, null)
            }
        }
    }
}
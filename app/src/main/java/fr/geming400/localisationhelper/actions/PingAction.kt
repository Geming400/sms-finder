package fr.geming400.localisationhelper.actions

import android.content.BroadcastReceiver
import android.content.Context
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.utils.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PingAction(name: String) : VoidAction(name) {
    override fun executeVoid(context: Context?) {

    }

    override fun checkValidity(rawContent: String): Boolean {
        return rawContent == PONG_MSG
    }

    override fun sendDataSMS(context: Context, sender: String) {
        this.smsSenderHelper(context, sender, PayloadType.DATA, PONG_MSG)
    }

    override fun onReceive(
        context: Context,
        sender: String,
        pendingResult: BroadcastReceiver.PendingResult,
        stage: Stage,
        rawContent: String
    ) {
        if (stage == Stage.RECEIVE_HOST) {
            val jsonDataStore = JsonDataStore(context)

            CoroutineScope(Dispatchers.IO.limitedParallelism(1, "PingAction's onReceive")).launch {
                val trackedContacts = jsonDataStore.trackedContactsFlow().first()

                val trackingData = jsonDataStore.getFirstTrackedContact(trackedContacts, sender)
                if (trackingData != null) {
                    val asContact = trackingData.getContact(context)

                    jsonDataStore.updateTrackedContact(asContact) {
                        it.copy(
                            lastPingAnswer = Timestamp.now()
                        )
                    }
                }
            }
        } else {
            this.sendDataSMS(context, sender)
        }
    }

    companion object {
        private const val PONG_MSG = "Pong !"
    }
}
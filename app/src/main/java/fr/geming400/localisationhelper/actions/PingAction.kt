package fr.geming400.localisationhelper.actions

import android.content.BroadcastReceiver
import android.content.Context
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.utils.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PingAction(name: String) : VoidAction(name) {
    override fun executeVoid(context: Context?) {

    }

    override fun checkValidity(rawContent: String): Boolean {
        return rawContent == PONG_MSG
    }

    override fun sendDataSMS(context: Context, sender: String, privateKey: String) {
        this.smsSenderHelper(context, sender, PayloadType.DATA, PONG_MSG, privateKey)
    }

    override fun onReceive(
        context: Context,
        sender: String,
        pendingResult: BroadcastReceiver.PendingResult,
        stage: Stage,
        trackingData: TrackingData,
        rawContent: String
    ) {
        if (stage == Stage.RECEIVE_HOST) {
            val jsonDataStore = JsonDataStore(context)

            CoroutineScope(Dispatchers.IO.limitedParallelism(1, "PingAction's onReceive (Stage.RECEIVE_HOST)")).launch {
                val asContact = trackingData.getContact(context)

                jsonDataStore.updateTrackedContact(asContact) {
                    it.copy(
                        lastPingAnswer = Timestamp.now()
                    )
                }
            }
        } else {
            this.sendDataSMS(context, sender, trackingData.privateKey!!)
        }
    }

    companion object {
        private const val PONG_MSG = "Pong !"
    }
}
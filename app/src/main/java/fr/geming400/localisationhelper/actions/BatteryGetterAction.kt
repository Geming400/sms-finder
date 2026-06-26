package fr.geming400.localisationhelper.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.ui.settings.Settings
import fr.geming400.localisationhelper.utils.BoxedTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class BatteryGetterAction(name: String): Action<Float>(name, Settings.BATTERY) {
    override fun serializeResult(obj: Float): String = obj.toString()

    @Throws(MalformedRawActionException::class)
    override fun parse(rawContent: String): Float {
        try {
            return rawContent.toFloat()
        } catch (e: NumberFormatException) {
            throw MalformedRawActionException(
                "Couldn't parse charge percentage float",
                e,
                this,
                rawContent
            )
        }
    }

    override fun execute(context: Context): CompletableFuture<Float> {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        return this.futureOf(batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        })
    }

    override fun onReceive(
        context: Context,
        sender: String,
        pendingResult: BroadcastReceiver.PendingResult,
        stage: Stage,
        trackingData: TrackingData,
        rawContent: String
    ) {
        val jsonDataStore = JsonDataStore(context)
        if (stage == Stage.RECEIVE_HOST) {
            val asContact = trackingData.getContact(context)

            val batteryCharge = parse(rawContent)

            CoroutineScope(Dispatchers.IO.limitedParallelism(1, "BatteryGetterAction's onReceive (Stage.RECEIVE_HOST)")).launch {
                jsonDataStore.updateTrackedContact(asContact) {
                    it.copy(
                        lastRecordedBatteryCharge = BoxedTimestamp.now(batteryCharge)
                    )
                }
            }
        } else {
            this.sendDataSMS(context, sender, trackingData.privateKey!!)
        }
    }
}
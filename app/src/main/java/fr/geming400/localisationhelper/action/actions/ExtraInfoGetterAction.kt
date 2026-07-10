package fr.geming400.localisationhelper.action.actions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import fr.geming400.localisationhelper.action.Action
import fr.geming400.localisationhelper.datastore.ExtraInfo
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.utils.BoxedTimestamp
import fr.geming400.localisationhelper.utils.Timestamp
import fr.geming400.localisationhelper.utils.toBoolFromInt
import fr.geming400.localisationhelper.utils.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

typealias SystemSettings = android.provider.Settings.System
typealias GlobalSettings = android.provider.Settings.Global

class ExtraInfoGetterAction(name: String) : Action<ExtraInfo>(name) {
    override fun serializeResult(obj: ExtraInfo): String =
        "${obj.isAirplaneModeEnabled.toInt()};${obj.isMobileDataEnabled.toInt()};${obj.isDndEnabled.toInt()}"

    override fun parse(rawContent: String): ExtraInfo {
        val vars = rawContent.split(";")

        return ExtraInfo(
            vars[0].toBoolFromInt(),
            vars[1].toBoolFromInt(),
            vars[2].toBoolFromInt()
        )
    }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    override fun execute(context: Context): CompletableFuture<ExtraInfo> {
        return this.futureOf {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            ExtraInfo(
                this.getBoolSetting(context, GlobalSettings.AIRPLANE_MODE_ON),
                telephonyManager.isDataEnabled,
                notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
        }
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

            val extraInfo = parse(rawContent)

            CoroutineScope(Dispatchers.IO.limitedParallelism(1, "ExtraInfoGetterAction's onReceive (Stage.RECEIVE_HOST)")).launch {
                jsonDataStore.updateTrackedContact(asContact) {
                    it.copy(
                        extraInfo = BoxedTimestamp.now(extraInfo),
                        lastPingAnswer = Timestamp.now()
                    )
                }
            }
        } else {
            this.sendDataSMS(context, sender, trackingData.privateKey!!)
        }
    }

    private fun getBoolSetting(context: Context, setting: String): Boolean =
        SystemSettings.getInt(context.contentResolver, setting, 0) != 0
}
package fr.geming400.localisationhelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import fr.geming400.localisationhelper.LogTags

// Stolen from https://github.com/geode-sdk/android-launcher/blob/758d5ad4320fa9c0cb21cf8eb145e0c81c07e32f/app/src/main/java/com/geode/launcher/InstallReceiver.kt
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val activityIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

                if (activityIntent != null) {
                    context.startActivity(activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // TODO: Remove because we put the apk in the cache
                // so this is not needed
                // clearDownloadedApks(context)
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(LogTags.AUTO_UPDATER, "failed to install update: $status, $msg")

                // TODO
//                val message = context.getString(R.string.launcher_self_update_failed, msg)
//                Toast.makeText(context, message, Toast.LENGTH_SHORT)
//                    .show()
            }
        }
    }
}
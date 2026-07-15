package fr.geming400.localisationhelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast
import fr.geming400.localisationhelper.AutoUpdater
import fr.geming400.localisationhelper.AutoUpdater.setState
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import kotlinx.coroutines.runBlocking
import kotlin.io.path.deleteExisting
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

// Stolen from https://github.com/geode-sdk/android-launcher/blob/758d5ad4320fa9c0cb21cf8eb145e0c81c07e32f/app/src/main/java/com/geode/launcher/InstallReceiver.kt
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val activityIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    else
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)

                if (activityIntent != null)
                    context.startActivity(activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> {
                runBlocking {
                    setState(null)
                }

                clearApks(context)
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(LogTags.AUTO_UPDATER, "failed to install update: $status, $msg")

                val message = context.getString(R.string.app_update_failed, msg)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearApks(context: Context) {
        val installDir = AutoUpdater.getApkInstallDir(context).toPath()
        installDir.walk()
            .filter { it.isRegularFile() && it.toFile().extension == "apk" }
            .forEach {
                Log.d(LogTags.AUTO_UPDATER, "Deleting apk file $it")
                it.deleteExisting()
            }
    }
}
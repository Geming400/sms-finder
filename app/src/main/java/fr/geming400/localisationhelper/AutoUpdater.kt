package fr.geming400.localisationhelper

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import fr.geming400.localisationhelper.receivers.InstallReceiver
import fr.geming400.localisationhelper.utils.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.IOUtils
import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


object AutoUpdater {
    const val REPO_NAME = "Geming400/sms-finder"

    val updateFlow: MutableSharedFlow<Boolean> = MutableSharedFlow()

    private var lastTimeChecked: Timestamp? = null
    private var repo: GHRepository? = null

    /**
     * Checks for an app update
     * @return If the app should get updated
     */
    suspend fun checkForUpdates(context: Context): Boolean {
        this.lastTimeChecked = Timestamp.now()

        val github = GitHub.connectAnonymously()
        this.repo = github.getRepository(REPO_NAME)

        val latestRelease = this.repo!!.latestRelease
        if (latestRelease == null) {
            Log.w(LogTags.AUTO_UPDATER, "Cannot check for updates as there are no releases")
            return false
        }

        try {
            val tagVersion = Version.parse(latestRelease.tagName)

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = Version.parse(packageInfo.versionName)

            val shouldGetUpdated = tagVersion.isHigherThan(appVersion)
            this.updateFlow.emit(shouldGetUpdated)

            return shouldGetUpdated
        } catch (e: ParseException) {
            Log.e(LogTags.AUTO_UPDATER, "Caught an error while trying to parse semver versions while trying to check for app update", e)
        }

        this.updateFlow.emit(false)
        return false
    }

    fun getApkFromRelease(release: GHRelease): GHAsset? {
        return try {
            release.listAssets().first { it.name.contains(".apk") }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun downloadApkFromAsset(context: Context, asset: GHAsset): Path {
        Log.i(LogTags.AUTO_UPDATER, "Downloading apk from ${asset.browserDownloadUrl}")

        val httpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(asset.browserDownloadUrl)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            val apkFile = File(context.cacheDir, asset.name)
            Files.write(apkFile.toPath(), IOUtils.toByteArray(response.body!!.byteStream()))
        }
    }

    // A lot of the stuff in this function (including the InstallReceiver broadcast receiver)
    // was stolen from https://github.com/geode-sdk/android-launcher/blob/758d5ad4320fa9c0cb21cf8eb145e0c81c07e32f/app/src/main/java/com/geode/launcher/main/UpdateComponents.kt
    @SuppressLint("RequestInstallPackagesPolicy")
    suspend fun updateApp(context: Context): Boolean {
        val shouldUpdate = this.checkForUpdates(context)
        if (!shouldUpdate)
            return false

        val asset = this.getApkFromRelease(this.repo!!.latestRelease)
        if (asset == null) {
            Log.e(LogTags.AUTO_UPDATER, "Wasn't able to find any asset with an apk file in the latest release")
            return false
        }

        val downloadedApkPath = this.downloadApkFromAsset(context, asset)
        val downloadedApkFile = downloadedApkPath.toFile()

        val installer = context.packageManager.packageInstaller

        // TODO: Maybe ?
//        installer.registerSessionCallback(object : PackageInstaller.SessionCallback() {
//            override fun onActiveChanged(sessionId: Int, active: Boolean) {
//                if (active) {
//                    _uiState.tryEmit(LauncherUpdateState.Active)
//                } else {
//                    _uiState.tryEmit(LauncherUpdateState.Began)
//                }
//            }
//
//            override fun onBadgingChanged(sessionId: Int) {}
//
//            override fun onCreated(sessionId: Int) {
//                _uiState.tryEmit(LauncherUpdateState.Began)
//            }
//
//            override fun onFinished(sessionId: Int, success: Boolean) {
//                _uiState.tryEmit(LauncherUpdateState.Finished)
//            }
//
//            override fun onProgressChanged(sessionId: Int, progress: Float) {
//                _uiState.tryEmit(LauncherUpdateState.Active)
//            }
//        })

        withContext(Dispatchers.IO) { runInterruptible {
            downloadedApkFile.inputStream().use { apkStream ->
                val length = downloadedApkFile.length()

                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL,
                )

                val sessionId = installer.createSession(params)
                installer.openSession(sessionId).use { session ->
                    session.openWrite("package", 0, length).use { sessionStream ->
                        apkStream.copyTo(sessionStream)
                        session.fsync(sessionStream)
                    }

                    val intent = Intent(context, InstallReceiver::class.java)
                    val pi = PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    session.commit(pi.intentSender)
                }
            }
        }}

        return true
    }

    fun getLastUpdateCheckTimestamp(): Timestamp? = this.lastTimeChecked
}
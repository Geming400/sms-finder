package fr.geming400.localisationhelper.ui.activities

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.util.Consumer
import kotlin.collections.forEachIndexed

open class CustomActivity : ComponentActivity() {
    typealias Callback = Consumer<Map<String, Boolean>>

    private var permissionCallbacks: MutableMap<Int, Array<Callback>> = mutableMapOf()

    @Deprecated("I don't care")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionCallbacks.getOrDefault(requestCode, arrayOf()).forEach {
            if (permissions.isEmpty())
                return

            val permissionsToSend = mutableMapOf<String, Boolean>()
            permissions.forEachIndexed { index, permission ->
                permissionsToSend[permission] = grantResults[index] == PackageManager.PERMISSION_GRANTED
            }

            it.accept(permissionsToSend)
        }
    }

    fun requestPermissionsWithCallback(permissions: Array<String>, requestCode: Int, callback: Callback) {
        var callbacks = permissionCallbacks.getOrDefault(requestCode, arrayOf())
        callbacks += callback

        permissionCallbacks[requestCode] = callbacks
        requestPermissions(permissions, requestCode)
    }
}
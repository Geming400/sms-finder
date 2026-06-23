package fr.geming400.localisationhelper.ui.activities

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.uuid.Uuid

open class PermissionsWithCallbackActivity : ComponentActivity() {
    typealias Callback = (Boolean) -> Unit
    typealias CallbackWithPermID = (String, Boolean) -> Unit

    private val permissionCallbacks: MutableMap<Uuid, CallbackWithPermID> = mutableMapOf()
    private val permissionsCallbacks: MutableMap<Uuid, (Map<String, Boolean>) -> Boolean> = mutableMapOf()

    // We use RequestMultiplePermissions instead of RequestPermission
    // as a workaround to get the permission ID
    val permissionLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        res.forEach { (permID, granted) ->
            permissionCallbacks.forEach { (id, callback) ->
                callback(permID, granted)
            }
        }
    }

    fun requestPermission(permission: String) {
        permissionLauncher.launch(arrayOf(permission))
    }

    fun requestPermissionWithCallback(permissions: String, callback: Callback) {
        val callbackID = Uuid.random()
        addPermissionCallback(
            permissions,
            callbackID
        ) { granted ->
            callback(granted)
            permissionCallbacks.remove(callbackID)
        }

        requestPermission(permissions)
    }

    fun requestPermissionsWithCallback(permissions: Collection<String>, callback: Callback) {
        val callbackID = Uuid.random()
        addPermissionsCallback(
            callbackID
        ) { res ->
            // If all the permissions were granted we return true
            callback(res.all { it.value })
            return@addPermissionsCallback permissionsCallbacks.remove(callbackID) != null
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun addPermissionCallback(permission: String, callbackID: Uuid, callback: Callback) =
        permissionCallbacks.put(callbackID) { perm, granted ->
            if (perm == permission)
                callback(granted)
        }

    fun addPermissionCallback(permission: String, callback: Callback) = addPermissionCallback(permission, Uuid.random(), callback)

    fun addPermissionsCallback(callbackID: Uuid, callback: (Map<String, Boolean>) -> Boolean) =
        permissionsCallbacks.put(callbackID, callback)
}
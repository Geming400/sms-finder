package fr.geming400.localisationhelper

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme


class MainActivity : ComponentActivity() {
    private val smsPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            RequestMultiplePermissions()
        ) { result ->
            val readGranted = result.getOrDefault(Manifest.permission.READ_SMS, false)
            val sendGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false)
            if (!(readGranted && sendGranted)) {
                createSmsPermissionDialog().show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        showSmsPermissionsDialog()
    }

    fun showSmsPermissionsDialog() {
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_SMS
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.SEND_SMS
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECEIVE_SMS
            )
        ) {
            Log.i(LogTags.SMS_PERMISSIONS, "Showing user popup to grant %s, %s and %s permission".format(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
            createSmsPermissionDialog().show()
        } else {
            Log.i(LogTags.SMS_PERMISSIONS, "User already granted %s, %s and %s permissions".format(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
        }
    }

    private fun requestSmsPermissions() {
        smsPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )
    }

    private fun createSmsPermissionDialog(): AlertDialog = AlertDialog.Builder(this)
        .setTitle("Permission required")
        .setMessage("This app needs SMS related permissions to function properly")
        .setPositiveButton(
            "Ok"
        ) { _, _ -> requestSmsPermissions() }
        .create()
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
package fr.geming400.localisationhelper.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private val requestedPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            // If the user didn't allow for fine location, we will force them to accept it
            if (!result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                requestLocationPermissions()
            } else {
                for (perm in requestedPermissions) {
                    // If at least one permission isn't granted we show the dialog
                    // telling the user to enable the required permissions
                    if (!result.getOrDefault(perm, false)) {
                        createPermissionsDialog().show()
                        break
                    }
                }
            }
        }

    private val locationPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }


    fun showPermissionsDialog() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            createLocationPermissionDialog().show()
        }

        // If at least one permission isn't granted
        if (
            requestedPermissions.map { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }.contains(true)
        ) {
            Log.i(LogTags.SMS_PERMISSIONS, "Showing user popup to grant %s permission".format(requestedPermissions.contentToString()))
            createPermissionsDialog().show()
        } else {
            Log.i(LogTags.SMS_PERMISSIONS, "User already granted %s permissions".format(requestedPermissions.contentToString()))
        }
    }

    private fun requestBasePermissions() {
        permissionsLauncher.launch(requestedPermissions)
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    private fun createPermissionsDialog(): AlertDialog = AlertDialog.Builder(this)
        .setTitle("Permissions required")
        .setMessage("This app needs SMS related permissions/location to function properly")
        .setPositiveButton(
            "Ok"
        ) { _, _ -> requestBasePermissions() }
        .setCancelable(false)
        .create()

    private fun createLocationPermissionDialog() : AlertDialog = AlertDialog.Builder(this)
        .setTitle("Location permissions")
        .setMessage("Fine location is preferred overed coarse location for more accurate results")
        .setPositiveButton(
            "Ok"
        ) { _, _ -> requestLocationPermissions() }
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                LocalisationHelperApp()
            }
        }

        showPermissionsDialog()
    }
}

@Composable
fun LocalisationHelperApp() {
    val context = LocalContext.current

    val appData = runBlocking {
        context.dataStore.data.first()
    }

    val openXiaomiNoticeDialog = remember { mutableStateOf(!appData.sawXiaomiNotice) }
    when {
        openXiaomiNoticeDialog.value -> {
            XiaomiNoticeDialog() {
                openXiaomiNoticeDialog.value = false
                runBlocking {
                    context.dataStore.updateData {
                        it.copy(sawXiaomiNotice = true)
                    }
                }
            }
        }
    }

    ActivitySelector(AppDestinations.HOME) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            PasswordInputDialog(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun PasswordInputDialog(modifier: Modifier) {
    val context = LocalContext.current

    var isPasswordFieldEnabled by remember {
        runBlocking {
            val appData = context.dataStore.data.first()
            mutableStateOf(appData.passwordHash == null)
        }
    }
    val inputState = rememberTextFieldState(if (isPasswordFieldEnabled) "" else "Hi this is a cool app")

    Column(modifier = modifier) {
        OutlinedSecureTextField(
            state = inputState,
            label = { Text(stringResource(R.string.enter_password)) },
            enabled = isPasswordFieldEnabled
        )

        Button(
            onClick = {
                isPasswordFieldEnabled = false

                runBlocking {
                    context.dataStore.updateData {
                        it.copy(passwordHash = Utils.hashString("SHA-256", (inputState.text as String).toByteArray()))
                    }
                }
            },
            enabled = isPasswordFieldEnabled
        ) {
            Text(stringResource(R.string.save_password))
        }

        Button(
            onClick = {
                runBlocking {
                    context.dataStore.updateData {
                        it.copy(passwordHash = null)
                    }
                }

                inputState.clearText()
                isPasswordFieldEnabled = true
            },
            enabled = !isPasswordFieldEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.reset_password))
        }
    }
}

@Composable
private fun XiaomiNoticeDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(stringResource(R.string.warning))
        },
        text = {
            Text(stringResource(R.string.xiaomi_notice))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}
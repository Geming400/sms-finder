package fr.geming400.localisationhelper.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.ActivityCompat
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.actions.Actions
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.ui.composable.ActivitySelector
import fr.geming400.localisationhelper.ui.composable.AppDestinations

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
                LocalisationHelperApp(this)
            }
        }

        showPermissionsDialog()
    }
}

@Composable
fun LocalisationHelperApp(context: Context) {
    ActivitySelector(context, AppDestinations.HOME) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ActionInputComponent(
                modifier = Modifier.padding(innerPadding),
                context = context
            )
        }
    }
}

@Composable
fun ActionInputComponent(modifier: Modifier, context: Context) {
    val inputState = rememberTextFieldState()
    var isPasswordFieldEnabled by remember { mutableStateOf(true) }


    Column(modifier = modifier) {
        OutlinedSecureTextField(
            state = inputState,
            label = { Text("Action name") },
            enabled = isPasswordFieldEnabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        val action = Actions.getByNameTypeless(inputState.text.toString())
        if (action == null) {
            Text("no action by the name " + inputState.text)
        } else {
            Text("Action output: " + action.execute(context))
        }

        Button(
            onClick = { isPasswordFieldEnabled = false },
            enabled = isPasswordFieldEnabled
        ) {
            Text("Save password")
        }

        Button(
            onClick = {
                inputState.clearText()
                isPasswordFieldEnabled = true
            },
            enabled = !isPasswordFieldEnabled
        ) {
            Text("Reset password")
        }
    }
}
package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.MainIntroductionComponent
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.Utils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : PermissionsWithCallbackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                MainIntroductionComponent()
                // TODO: Condition for showing LocalisationHelperApp
//                LocalisationHelperApp()
            }
        }

    }
}

@Composable
private fun LocalisationHelperApp() {
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
            PasswordInputField(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PasswordInputField(modifier: Modifier) {
    val context = LocalContext.current

    var isPasswordFieldEnabled by remember {
        runBlocking {
            val appData = context.dataStore.data.first()
            mutableStateOf(appData.passwordHash == null)
        }
    }
    val passwordInputState = rememberTextFieldState(if (isPasswordFieldEnabled) "" else "Hi this is a cool app")

    Column(modifier = modifier) {
        OutlinedSecureTextField(
            state = passwordInputState,
            label = { Text(stringResource(R.string.enter_password)) },
            enabled = isPasswordFieldEnabled,
            isError = !Utils.isPasswordValid(passwordInputState.text) && isPasswordFieldEnabled
        )

        Button(
            onClick = {
                isPasswordFieldEnabled = false

                runBlocking {
                    context.dataStore.updateData {
                        it.copy(passwordHash = Utils.hashString("SHA-256", (passwordInputState.text as String).toByteArray()))
                    }
                }
            },
            enabled = isPasswordFieldEnabled && Utils.isPasswordValid(passwordInputState.text)
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

                passwordInputState.clearText()
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
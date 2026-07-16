package fr.geming400.localisationhelper.ui.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.OrderedScatterSet
import androidx.collection.orderedScatterSetOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.app.ActivityCompat
import contacts.core.Contacts
import contacts.core.LookupQuery
import fr.geming400.localisationhelper.AutoUpdater
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.LocalisationHelperData
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.CategoryCard
import fr.geming400.localisationhelper.ui.components.ContactProfile
import fr.geming400.localisationhelper.ui.components.LoadingCircle
import fr.geming400.localisationhelper.ui.components.MainIntroductionComponent
import fr.geming400.localisationhelper.ui.components.Step
import fr.geming400.localisationhelper.ui.components.rememberCurrentStep
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.utils.centerHorizontally
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : PermissionsWithCallbackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                val coroutineScope = rememberCoroutineScope()
                val appData by this.dataStore.data
                    .collectAsState(initial = null, coroutineScope.coroutineContext)

                if (appData == null) {
                    LoadingCircle()
                } else {
                    if (!areAllPermissionsGranted(this) || appData!!.firstTimeOpening) {
                        val currentStep = if (appData!!.firstTimeOpening)
                            null
                        else
                            rememberCurrentStep(Step.PERMISSIONS)

                        MainIntroductionComponent(currentStep) {
                            runBlocking {
                                dataStore.updateData {
                                    it.copy(firstTimeOpening = false)
                                }
                            }
                        }
                    } else {
                        if (!AutoUpdater.hasDoneInitCheck) {
                            AutoUpdater.hasDoneInitCheck = true
                            this.checkForUpdates()
                        }

                        LocalisationHelperApp(appData!!)
                    }
                }
            }
        }
    }

    fun checkForUpdates() {
        Log.i(LogTags.AUTO_UPDATER, "Checking for updates in MainActivity on startup")

        val thread = Thread() {
            runBlocking {
                AutoUpdater.checkForUpdates(this@MainActivity)
            }
        }

        thread.name = "Updater Checker thread (checker)"
        thread.start()
    }

    companion object {
        val requiredPermissions: OrderedScatterSet<String> = orderedScatterSetOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        fun areAllPermissionsGranted(context: Context) =
            requiredPermissions.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }
}

@Composable
private fun LocalisationHelperApp(appData: LocalisationHelperData) {
    val context = LocalContext.current

    var openAppAsBgServiceNotice by rememberSaveable { mutableStateOf(!appData.sawBgServiceNotice) }
    when {
        openAppAsBgServiceNotice -> {
            AppAsBgServiceNotice() {
                openAppAsBgServiceNotice = false
                runBlocking {
                    context.dataStore.updateData {
                        it.copy(sawBgServiceNotice = true)
                    }
                }
            }
        }
    }

    ActivitySelector(AppDestinations.HOME) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                Card(
                    modifier = Modifier.padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        modifier = Modifier
                            .centerHorizontally()
                            .padding(5.dp),
                        text = stringResource(R.string.leak_pii_disclaimer),
                        textAlign = TextAlign.Center,
                        fontSize = 4.em,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                CategoryCard(stringResource(R.string.password)) {
                    PasswordInputField()
                }

                CategoryCard(stringResource(R.string.recent_contacts)) {
                    if (appData.lastAccessedContacts.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .centerHorizontally()
                                .alpha(0.65f)
                                .padding(4.dp),
                            text = stringResource(R.string.no_recent_contacts),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        appData.lastAccessedContacts.forEach {
                            val contact = Contacts(context)
                                .lookupQuery()
                                .whereLookupKeyWithIdMatches(LookupQuery.LookupKeyWithId(it, 0))
                                .find()
                                .first()

                            ContactProfile(contact = contact) {
                                UserTrackingActivity.start(context, contact)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordInputField(modifier: Modifier = Modifier) {
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

        Spacer(Modifier.height(10.dp))

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
private fun AppAsBgServiceNotice(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val hasXiaomiPhone = Build.MANUFACTURER == "Xiaomi"

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(stringResource(R.string.warning))
        },
        text = {
            Text(
                stringResource(
                    if (hasXiaomiPhone)
                        R.string.xiaomi_notice
                    else
                        R.string.app_as_bg_service_notice
                )
            )
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
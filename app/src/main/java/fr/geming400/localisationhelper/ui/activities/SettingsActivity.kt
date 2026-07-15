package fr.geming400.localisationhelper.ui.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import fr.geming400.localisationhelper.AutoUpdater
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.LocalisationHelperData
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.SettingScreen
import fr.geming400.localisationhelper.ui.components.SettingsCategory
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.centerHorizontally
import kotlinx.coroutines.runBlocking

class SettingsActivity : PermissionsWithCallbackActivity() {
    lateinit var snackbarHostState: SnackbarHostState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            this.snackbarHostState = remember { SnackbarHostState() }

            LocalisationHelperTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = this.snackbarHostState)
                    }
                ) { innerPadding ->
                    ActivitySelector(AppDestinations.SETTINGS) {
                        SettingScreen(modifier = Modifier.padding(innerPadding)) {
                            item {
                                SettingsCategory(title = stringResource(R.string.setting_category_update_checker)) {
                                    UpdateChecker()
                                }

                                SettingsCategory(title = stringResource(R.string.setting_category_about)) {
                                    AboutCategory()
                                }

                                SettingsCategory(title = stringResource(R.string.setting_category_debug)) {
                                    ResetAppButton()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetAppButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Button(
        modifier = modifier
            .centerHorizontally()
            .padding(8.dp),
        onClick = {
            runBlocking {
                context.dataStore.updateData {
                    LocalisationHelperData()
                }
            }

            Toast.makeText(context, R.string.reset_after_action, Toast.LENGTH_LONG).show()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(stringResource(R.string.reset_app))
    }
}

@Composable
private fun UpdateChecker() {
    val activity = LocalActivity.current as SettingsActivity
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val txtModifier = Modifier.padding(4.dp)

                Text(
                    modifier = txtModifier,
                    text = if (AutoUpdater.hasFoundUpdate())
                        stringResource(R.string.new_update_found)
                    else
                        stringResource(R.string.no_update_found),
                    textAlign = TextAlign.Center
                )

                if (AutoUpdater.getLastUpdateCheckTimestamp() != null)
                    Text(
                        modifier = txtModifier,
                        text = stringResource(R.string.found_update_time, AutoUpdater.getLastUpdateCheckTimestamp()!!.getRelativeTime()),
                        textAlign = TextAlign.Center
                    )

                val updaterState by AutoUpdater.stateFlow.collectAsState(AutoUpdater.getCurrentState(), coroutineScope.coroutineContext)
                if (updaterState.getLocalisedMessage() != null)
                    Text(
                        modifier = txtModifier,
                        text = updaterState.getLocalisedWithPrefixMessage()!!,
                        textAlign = TextAlign.Center
                    )
            }
        }

        Button(
            modifier = Modifier.padding(4.dp),
            onClick = {
                val thread = Thread() {
                    runBlocking {
                        AutoUpdater.checkForUpdates(activity)
                    }
                }

                thread.name = "Updater Checker thread (checker)"
                thread.start()
            }
        ) {
            Text(stringResource(R.string.check_for_updates))
        }

        val hasFoundUpdate by AutoUpdater.updateFlow
            .collectAsState(AutoUpdater.hasFoundUpdate(), coroutineScope.coroutineContext)

        Button(
            modifier = Modifier.padding(4.dp),
            onClick = {
                val thread = Thread() {
                    runBlocking {
                        AutoUpdater.updateApp(activity)
                    }
                }

                thread.name = "Updater Checker thread (updater)"
                thread.start()
            },
            enabled = hasFoundUpdate
        ) {
            Text(stringResource(R.string.download_update))
        }

        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun AboutCategory() {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.created_by),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    try {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/geming400/sms-finder".toUri()
                        )

                        context.startActivity(browserIntent)
                    } catch (e: ActivityNotFoundException) {
                        Log.w(LogTags.USER_TRACKING, "Couldn't find any activities to open a web browser", e)
                        Toast.makeText(context, R.string.no_app_for_intent, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null
                )

                Spacer(Modifier.width(12.dp))

                Text(stringResource(R.string.source_code))
            }
        }
    }
}
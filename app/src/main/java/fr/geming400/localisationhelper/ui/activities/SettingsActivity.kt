package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.LocalisationHelperData
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.SettingScreen
import fr.geming400.localisationhelper.ui.components.SettingsCategory
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.centerVertically
import kotlinx.coroutines.runBlocking

class SettingsActivity : PermissionsWithCallbackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ActivitySelector(AppDestinations.SETTINGS) {
                        SettingScreen(modifier = Modifier.padding(innerPadding)) {
                            item {
                                SettingsCategory(title = stringResource(R.string.setting_category_debug)) {
                                    ResetButton()
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
private fun ResetButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Button(
        modifier = modifier
            .centerVertically()
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
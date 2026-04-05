package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.SettingScreen
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme

class SettingsActivity : CustomActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ActivitySelector(AppDestinations.SETTINGS) {
                        SettingScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
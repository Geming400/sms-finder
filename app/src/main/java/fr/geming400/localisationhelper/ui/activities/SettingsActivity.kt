package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.geming400.localisationhelper.ui.composable.ActivitySelector
import fr.geming400.localisationhelper.ui.composable.AppDestinations

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ActivitySelector(this, AppDestinations.SETTINGS)
        }
    }
}
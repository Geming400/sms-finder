package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme

class TrackingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                ActivitySelector(AppDestinations.TRACKING)
            }
        }
    }
}
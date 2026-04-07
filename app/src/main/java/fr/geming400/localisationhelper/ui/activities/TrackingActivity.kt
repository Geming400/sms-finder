package fr.geming400.localisationhelper.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.ContactsList
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme

class TrackingActivity : ComponentActivity() {
    val contactPickerLauncher: ActivityResultLauncher<Void?> =
        registerForActivityResult(
            ActivityResultContracts.PickContact()
        ) { result ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                ActivitySelector(AppDestinations.TRACKING) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = { BottomBar() }
                    ) { innerPadding ->
                        ContactsList(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar() {
    BottomAppBar(
        actions = {

        },
        floatingActionButton = {
            ContactAdderButton()
        },
        containerColor = Color.Transparent
    )
}

@Composable
fun ContactAdderButton(modifier: Modifier = Modifier) {
    val trackingActivity = LocalActivity.current as TrackingActivity

    FloatingActionButton(
        modifier = modifier,
        onClick = {
            trackingActivity.contactPickerLauncher.launch()
        }
    ) {
        Text(
            "+",
            fontSize = 10.em
        )
    }
}
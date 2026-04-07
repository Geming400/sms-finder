package fr.geming400.localisationhelper.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.activities.MainActivity
import fr.geming400.localisationhelper.ui.activities.SettingsActivity
import fr.geming400.localisationhelper.ui.activities.TrackingActivity

@Composable
fun ActivitySelector(currentDestination: AppDestinations, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val context = LocalContext.current

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        if (currentDestination != it)
                            it.startActivity(context)
                    }
                )
            }
        }
    ) {
        content()
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
    val clazz: Class<out Activity>
) {
    /**
     * The main menu for the app
     */
    HOME("Home", R.drawable.ic_home, MainActivity::class.java),

    /**
     * Where you can track someone
     */
    TRACKING("Tracking", R.drawable.ic_radar, TrackingActivity::class.java),

    /**
     * The settings menu
     */
    SETTINGS("Settings", R.drawable.ic_settings, SettingsActivity::class.java);

    /**
     * Starts the activity of this destination
     * @param context the app context
     */
    fun startActivity(context: Context) {
        val intent = Intent(context, clazz)
        context.startActivity(intent)
    }
}
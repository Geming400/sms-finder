package fr.geming400.localisationhelper.ui.composable

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.activities.MainActivity
import fr.geming400.localisationhelper.ui.activities.SettingsActivity
import fr.geming400.localisationhelper.ui.activities.TrackingActivity

@Composable
fun ActivitySelector(context: Context, currentDestination: AppDestinations, content: @Composable () -> Unit = {}) {
    NavigationSuiteScaffold(
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
    HOME("Home", R.drawable.ic_home, MainActivity::class.java),
    TRACKING("Tracking", R.drawable.ic_radar, TrackingActivity::class.java),
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
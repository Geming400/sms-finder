package fr.geming400.localisationhelper.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                            contentDescription = stringResource(it.label)
                        )
                    },
                    label = { Text(stringResource(it.label)) },
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
    @field:StringRes
    val label: Int,
    @field:DrawableRes
    val icon: Int,
    val clazz: Class<out Activity>
) {
    /**
     * The main menu for the app
     */
    HOME(R.string.destinations_home, R.drawable.ic_home, MainActivity::class.java),

    /**
     * Where you can track someone
     */
    TRACKING(R.string.destinations_tracking, R.drawable.ic_radar, TrackingActivity::class.java),

    /**
     * The settings menu
     */
    SETTINGS(R.string.destinations_settings, R.drawable.ic_settings, SettingsActivity::class.java);

    /**
     * Starts the activity of this destination
     * @param context the app context
     */
    fun startActivity(context: Context) {
        val intent = Intent(context, clazz)
        context.startActivity(intent)
    }

    /**
     * Starts the activity of this destination
     * @param context the app context
     */
    fun startActivity(context: Context, bundle: Bundle) {
        val intent = Intent(context, clazz)
        context.startActivity(intent, bundle)
    }

    fun isLeft(other: AppDestinations): Boolean {
        return this.ordinal < other.ordinal
    }

    fun isRight(other: AppDestinations): Boolean {
        return this.ordinal > other.ordinal
    }
}
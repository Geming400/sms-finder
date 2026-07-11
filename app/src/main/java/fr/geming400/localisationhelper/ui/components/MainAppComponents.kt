package fr.geming400.localisationhelper.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.datastore.JsonDataStore
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

@Composable
fun rememberJsonDatastore(): JsonDataStore {
    val context = LocalContext.current
    return remember { JsonDataStore(context) }
}

@Composable
fun LoadingCircle(modifier: Modifier = Modifier) {
    // We recreate a new Scaffold
    // because if we don't our theme won't be applied
    // so for phones in dark mode, the bg won't be seen
    // as dark
    //
    // We can't put the following Box in the ActivitySelector's content either
    // because since it's a loading screen we don't want to
    // have the selector menu in the bottom
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                CircularProgressIndicator(
                    modifier = Modifier.size(65.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(modifier.padding(8.dp)) {
            Text(
                text = "$name:",
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold,
                fontSize = 4.em,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

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
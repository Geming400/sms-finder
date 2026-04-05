package fr.geming400.localisationhelper.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.preference.PreferenceManager
import fr.geming400.localisationhelper.Utils
import fr.geming400.localisationhelper.ui.activities.CustomActivity
import fr.geming400.localisationhelper.ui.settings.Setting
import fr.geming400.localisationhelper.ui.settings.Settings

@Composable
fun SettingScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier
            .scrollable(state = ScrollState(0), orientation = Orientation.Horizontal))
    {
        Settings.getSettingsByCategory().forEach { (category, settings) ->
            if (category.isHidden) {
                item {
                    SettingsCategory(title = category.name) {
                        settings.forEach {
                            SettingItem(setting =  it)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(modifier: Modifier = Modifier, setting: Setting<*>) {
    when (setting) {
        is Setting.BooleanSetting -> BooleanSetting(modifier, setting)
        is Setting.IntSetting -> IntSetting(modifier, setting)
        is Setting.FloatSetting -> FloatSetting(modifier, setting)
        is Setting.StringSetting -> StringSetting(modifier, setting)
        else -> {
            Text(
                "Unsupported setting: ${setting.id}",
                color = Color(255, 0, 0)
            )
        }
    }
}

@Composable
fun SettingSpacing(modifier: Modifier = Modifier, setting: Setting<*>, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }

    if (setting.description.isPresent)
        SettingDescription(modifier = modifier, description = setting.description.get())
}

@Composable
fun SettingsCategory(modifier: Modifier = Modifier, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
        )

        content()
    }
}

@Composable
fun SettingDescription(modifier: Modifier = Modifier, description: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .offset(y = (-10).dp)
    ) {
        Text(
            description,
            fontSize = 4.em,
            color = Color(180, 180, 180, 200)
        )
    }
}

@Composable
fun BooleanSetting(modifier: Modifier = Modifier, setting: Setting<Boolean>) {
    val context = LocalContext.current
    val activity = LocalActivity.current as CustomActivity
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var isVisuallyChecked by remember { mutableStateOf(sharedPreferences.getBoolean(setting.id, setting.defaultValue)) }

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.name,
            style = MaterialTheme.typography.bodyLarge
        )

        Checkbox(
            checked = isVisuallyChecked,
            onCheckedChange = {
                setting.askForPermissions(activity, it) {
                    isVisuallyChecked = it
                }
            }
        )
    }
}

@Composable
fun IntSetting(modifier: Modifier = Modifier, setting: Setting<Int>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getInt(setting.id, setting.defaultValue).toString())

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.name,
            style = MaterialTheme.typography.bodyLarge
        )

        TextField(
            state = textState,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            inputTransformation = {
                try {
                    setting.setValue(context, asCharSequence().toString().toInt())
                } catch (_: NumberFormatException) {

                }
            },
            isError = !Utils.isInt(textState.text.toString())
        )
    }
}

@Composable
fun FloatSetting(modifier: Modifier = Modifier, setting: Setting<Float>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getFloat(setting.id, setting.defaultValue).toString())

    val isFloat = textState.text.toString().toFloatOrNull() != null

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.name,
            style = MaterialTheme.typography.bodyLarge
        )

        // TODO: add setting description (when setting setup is finished)
        TextField(
            state = textState,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            inputTransformation = {
                try {
                    setting.setValue(context, asCharSequence().toString().toFloat())
                } catch (_: NumberFormatException) {

                }
            },
            isError = !isFloat
        )
    }
}

@Composable
fun StringSetting(modifier: Modifier = Modifier, setting: Setting<String>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getString(setting.id, setting.defaultValue)!!)

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.name
        )

        TextField(
            state = textState,
            inputTransformation = {
                setting.setValue(context, textState.text.toString())
            }
        )
    }
}
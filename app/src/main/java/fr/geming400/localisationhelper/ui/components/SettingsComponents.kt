package fr.geming400.localisationhelper.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.activities.PermissionsWithCallbackActivity
import fr.geming400.localisationhelper.ui.settings.Setting
import fr.geming400.localisationhelper.ui.settings.Settings
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.utils.centerHorizontally

@Composable
fun SettingScreen(modifier: Modifier, content: LazyListScope.() -> Unit = {}) {
    LazyColumn(
        modifier = modifier
            .scrollable(state = ScrollState(0), orientation = Orientation.Horizontal))
    {
        Settings.getSettingsByCategory().forEach { (category, settings) ->
            if (!category.isHidden) {
                item {
                    SettingsCategory(title = category.name) {
                        settings.forEach {
                            SettingItem(setting = it)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        content()
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
                color = Color.Red
            )
        }
    }
}

@Composable
private fun SettingSpacing(modifier: Modifier = Modifier, setting: Setting<*>, content: @Composable () -> Unit) {
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
        SettingDescription(modifier = modifier, description = stringResource(setting.description.get()))
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
private fun SettingDescription(modifier: Modifier = Modifier, description: String) {
    Text(
        modifier = modifier
            .centerHorizontally()
            .padding(horizontal = 6.dp)
            .offset(y = (-10).dp),
        text = description,
        color = Color(180, 180, 180, 200),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(maxFontSize = 22.sp)
    )
}

@Composable
private fun BooleanSetting(modifier: Modifier = Modifier, setting: Setting<Boolean>) {
    val context = LocalContext.current
    val activity = LocalActivity.current as PermissionsWithCallbackActivity
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var isVisuallyChecked by remember { mutableStateOf(sharedPreferences.getBoolean(setting.id, setting.defaultValue)) }

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.getName(LocalResources.current),
            style = MaterialTheme.typography.bodyLarge
        )

        var shouldShowGrantingDialog by remember { mutableStateOf(false) }
        when {
            shouldShowGrantingDialog -> {
                GrantPermissionsDialog(
                    modifier = modifier,
                    permissions = setting.requiredPermissions,
                    dismissButton = {
                        Button(
                            onClick = {
                                shouldShowGrantingDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                }) {
                    shouldShowGrantingDialog = false
                }
            }
        }

        Switch(
            checked = isVisuallyChecked,
            onCheckedChange = {
                setting.askForPermissions(activity, it) { success ->
                    if (success) {
                        isVisuallyChecked = it
                    } else {
                        shouldShowGrantingDialog = true
                    }
                }
            }
        )
    }
}

@Composable
private fun IntSetting(modifier: Modifier = Modifier, setting: Setting<Int>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getInt(setting.id, setting.defaultValue).toString())

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.getName(LocalResources.current),
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
private fun FloatSetting(modifier: Modifier = Modifier, setting: Setting<Float>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getFloat(setting.id, setting.defaultValue).toString())

    val isFloat = textState.text.toString().toFloatOrNull() != null

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.getName(LocalResources.current),
            style = MaterialTheme.typography.bodyLarge
        )

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
private fun StringSetting(modifier: Modifier = Modifier, setting: Setting<String>) {
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val textState = rememberTextFieldState(sharedPreferences.getString(setting.id, setting.defaultValue)!!)

    SettingSpacing(modifier, setting) {
        Text(
            text = setting.getName(LocalResources.current)
        )

        TextField(
            state = textState,
            inputTransformation = {
                setting.setValue(context, textState.text.toString())
            }
        )
    }
}
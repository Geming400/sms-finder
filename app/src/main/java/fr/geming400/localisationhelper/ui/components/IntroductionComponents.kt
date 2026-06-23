package fr.geming400.localisationhelper.ui.components

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.activities.MainActivity
import fr.geming400.localisationhelper.ui.settings.Settings
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.utils.centerHorizontally

@Composable
fun MainIntroductionComponent(currentStepState: MutableState<Step>? = null, onEnd: () -> Unit = {}) {
    var currentStep by currentStepState ?: rememberCurrentStep()

    Scaffold() { innerPadding ->
        Column(
            Modifier.padding(innerPadding)
        ) {
            StepsBar(currentStep = currentStep)
            HorizontalDivider(thickness = 3.dp)

            val scrollState = rememberScrollState()

            Spacer(Modifier.height(25.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    var isButtonEnabled = currentStep.component()

                    var shouldReloadButtonEnablingState by remember { mutableStateOf(false) }
                    when {
                        shouldReloadButtonEnablingState -> {
                            isButtonEnabled = currentStep.component()
                            shouldReloadButtonEnablingState = false
                        }
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME)
                                shouldReloadButtonEnablingState = true
                        }

                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 15.dp))
                    NextButton(currentStep = currentStep, isEnabled = isButtonEnabled) { step, isEnd ->
                        if (isEnd) {
                            onEnd()
                        } else {
                            currentStep = step!!
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCurrentStep(initialStep: Step = Step.INTRODUCTION): MutableState<Step> =
    remember { mutableStateOf(initialStep) }

@Composable
private fun StepsBar(
    modifier: Modifier = Modifier,
    currentStep: Step
) {
    Row(modifier = modifier.fillMaxWidth()) {
        val height = 50.dp

        for (step in Step.entries) {
            Text(
                text = stringResource(step.nameID),
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                color = if (step == currentStep) Color.Unspecified else MaterialTheme.colorScheme.onSecondary
            )

            if (step != Step.entries.last())
                VerticalDivider(
                    modifier = Modifier.height(height),
                    thickness = 3.dp
                )
        }
    }
}

@Composable
private fun NextButton(modifier: Modifier = Modifier, currentStep: Step, isEnabled: Boolean, onClick: (Step?, Boolean) -> Unit) {
    Button(
        modifier = modifier
            .centerHorizontally(),
        onClick = {
            if (currentStep == Step.entries.last()) {
                onClick(null, true)
            } else {
                onClick(Step.entries[currentStep.ordinal + 1], false)
            }
        },
        enabled = isEnabled
    ) {
        Text(stringResource(R.string.next))
    }
}

@Composable
private fun IntroductionMainComponent(): Boolean {
    Text(
        modifier = Modifier
            .padding(8.dp),
        text = stringResource(R.string.introduction_steps_introduction_primary),
        textAlign = TextAlign.Center
    )

    Text(
        modifier = Modifier
            .padding(8.dp),
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Bold,
        fontSize = 6.em,
        text = stringResource(R.string.introduction_steps_introduction_how_does_this_work),
        textAlign = TextAlign.Center
    )

    Text(
        modifier = Modifier
            .padding(8.dp),
        text = stringResource(R.string.introduction_steps_introduction_how_does_this_work_content),
        textAlign = TextAlign.Center
    )

    return true
}

@Composable
private fun PermissionMainComponent(): Boolean {
    val context = LocalContext.current

    Text(
        modifier = Modifier
            .padding(8.dp),
        text = stringResource(R.string.must_grant_permissions),
        textAlign = TextAlign.Center
    )

    MainActivity.requiredPermissions.forEach {
        PermissionButton(permission = it)
    }

    return MainActivity.areAllPermissionsGranted(context)
}

@SuppressLint("RestrictedApi")
@Composable
private fun PermissionButton(modifier: Modifier = Modifier, permission: String) {
    val activity = LocalActivity.current!! as MainActivity
    val context = LocalContext.current

    val permissionInfo = remember(permission) {
        context.packageManager.getPermissionInfo(
            permission,
            PackageManager.GET_META_DATA
        )
    }

    val permissionName = remember(permission) {
        permissionInfo.loadLabel(context.packageManager)
    }

    var isPermissionGranted by remember { mutableStateOf(
        ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    ) }

    activity.addPermissionCallback(permission) {
        isPermissionGranted = it
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Live refresh for when the user goes to the
    // settings app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                isPermissionGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    var shouldShowGrantingDialog by remember { mutableStateOf(false) }
    when {
        shouldShowGrantingDialog -> {
            GrantPermissionDialog(modifier, permission) {
                @Suppress("AssignedValueIsNeverRead")
                shouldShowGrantingDialog = false
            }
        }
    }

    val buttonColors = if (isPermissionGranted)
        ButtonDefaults.buttonColors(
            disabledContainerColor = Color(0xFF204B21)
        )
    else
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = Color.White
        )

    Button(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 3.dp)
            .centerHorizontally(),
        onClick = {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                activity.requestPermission(permission)
            } else if (!isPermissionGranted) {
                @Suppress("AssignedValueIsNeverRead")
                shouldShowGrantingDialog = true
            }
        },
        colors = buttonColors,
        enabled = !isPermissionGranted
    ) {
        Text(
            text = permissionName as String,
            fontWeight = if (isPermissionGranted) null else FontWeight.Bold
        )
    }
}

@Composable
fun GrantPermissionDialog(modifier: Modifier = Modifier, permission: String, onEnd: () -> Unit) {
    val activity = LocalActivity.current!! as MainActivity
    val context = LocalContext.current

    val permissionInfo = remember { context.packageManager.getPermissionInfo(permission, 0) }
    val permissionName = remember { permissionInfo.loadLabel(context.packageManager) }

    val isPermissionGranted = ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    if (!isPermissionGranted) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {
                onEnd()
            },
            title = {
                Text(stringResource(R.string.denied_permission, permissionName))
            },
            text = {
                Text(stringResource(R.string.must_authorize_permission_reason))
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            activity.requestPermission(permission)
                        } else {
                            Utils.openSettingAppForActivity(activity)
                        }

                        onEnd()
                    }
                ) {
                    Text(stringResource(R.string.grant))
                }
            }
        )
    }
}

@Composable
fun GrantPermissionsDialog(modifier: Modifier = Modifier, permissions: Collection<String>, dismissButton: (@Composable () -> Unit)?, onEnd: () -> Unit) {
    val activity = LocalActivity.current!! as MainActivity
    val context = LocalContext.current

    val permissionNames = StringBuilder()
    val isOnePermissionDenied = permissions.any { ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_DENIED }

    permissions.forEach {
        val permissionInfo = remember(it) { context.packageManager.getPermissionInfo(it, 0) }
        val permissionName = remember { permissionInfo.loadLabel(context.packageManager) }

        if (it == permissions.first()) {
            permissionNames.append("$permissionName")
        } else {
            permissionNames.append(", $permissionName")
        }
    }

    if (isOnePermissionDenied) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {
                onEnd()
            },
            title = {
                Text(stringResource(R.string.denied_permissions))
            },
            text = {
                Text(stringResource(R.string.must_authorize_permissions_reason, permissionNames.toString()))
            },
            dismissButton = dismissButton,
            confirmButton = {
                Button(
                    onClick = {
                        val shouldShowRationale = permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

                        if (shouldShowRationale) {
                            activity.requestPermissionsWithCallback(permissions) { success ->
                                if (!success)
                                    Utils.openSettingAppForActivity(activity)
                            }
                        } else {
                            Utils.openSettingAppForActivity(activity)
                        }

                        onEnd()
                    }
                ) {
                    Text(stringResource(R.string.grant))
                }
            }
        )
    }
}

@Composable
private fun ActionSettingsMainComponent(): Boolean {
    Text(
        modifier = Modifier
            .centerHorizontally()
            .padding(8.dp),
        text = stringResource(R.string.introduction_steps_settings_content),
        textAlign = TextAlign.Center
    )

    for (setting in Settings.getSettings()) {
        if (setting.areActionsDependant()) {
            SettingItem(setting = setting)
        }
    }

    return true
}

enum class Step(@field:StringRes val nameID: Int, val component: @Composable () -> Boolean) {
    INTRODUCTION(R.string.introduction_steps_introduction, component = { IntroductionMainComponent() }),
    PERMISSIONS(R.string.introduction_steps_permissions, component = { PermissionMainComponent() }),
    SETTINGS(R.string.destinations_settings, component = { ActionSettingsMainComponent() })
}
package fr.geming400.localisationhelper.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import contacts.core.Contacts
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.actions.Actions
import fr.geming400.localisationhelper.actions.BaseAction
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.SerializableGeolocation
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.DeletableContactProfile
import fr.geming400.localisationhelper.ui.components.LoadingCircle
import fr.geming400.localisationhelper.ui.components.PhoneNumberDropdown
import fr.geming400.localisationhelper.ui.components.rememberJsonDatastore
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import fr.geming400.localisationhelper.utils.Utils
import kotlinx.coroutines.runBlocking
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.seconds

// TODO: Use snackbar when sending action request
class UserTrackingActivity : ComponentActivity() {
    lateinit var contact: Contact

    lateinit var snackbarHostState: SnackbarHostState

    lateinit var trackedContacts: List<TrackingData>

    var isContactBeingDeleted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras!!

        val lookupKey = bundle.getString("lookupKey")!!
        val contactID = bundle.getLong("contactID")

        Log.i(LogTags.USER_TRACKING, "Fetching contact from which this activity was started")
        contact = getContact(LookupQuery.LookupKeyWithId(lookupKey, contactID))
        Log.i(LogTags.USER_TRACKING, "Found contact $contact !")

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                val coroutineScope = rememberCoroutineScope()
                val jsonDataStore = remember(this) { JsonDataStore(this) }

                val trackedContacts by jsonDataStore.trackedContactsFlow()
                    .collectAsState(initial = emptyList(), coroutineScope.coroutineContext)
                this.trackedContacts = trackedContacts

                // Since we're tracking a contact, we're waiting until 'trackedContacts' is non-empty
                if (trackedContacts.isEmpty() || isContactBeingDeleted) {
                    LoadingCircle()

                    return@LocalisationHelperTheme
                }

                val trackedContactInfo = jsonDataStore.getTrackedContact(trackedContacts, contact)

                snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                ) { innerPadding ->
                    val shouldShowMap = remember { mutableStateOf(false) }
                    if (shouldShowMap.value) {
                        UserLocationMap(Modifier.padding(innerPadding), trackedContactInfo) {
                            shouldShowMap.value = false
                        }
                    } else {
                        ActivitySelector(AppDestinations.TRACKING) {
                            if (trackedContactInfo.privateKey == null) {
                                ChangeContactPrivateKeyDialog(contact = contact, trackedContactInfo = trackedContactInfo) {
                                    this.startActivity(Intent(this, TrackingActivity::class.java))
                                }
                            } else {
                                MainUserTrackingComponent(
                                    innerPadding = innerPadding,
                                    contact = contact,
                                    trackedContactInfo = trackedContactInfo,
                                    jsonDataStore = jsonDataStore
                                ) {
                                    shouldShowMap.value = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getContact(lookupKeyWithId: LookupQuery.LookupKeyWithId): Contact =
        Contacts(this)
            .lookupQuery()
            .whereLookupKeyWithIdMatches(lookupKeyWithId)
            .find()
            .first()

    @Composable
    private fun MainUserTrackingComponent(
        innerPadding: PaddingValues,
        contact: Contact,
        trackedContactInfo: TrackingData,
        jsonDataStore: JsonDataStore = JsonDataStore(this),
        onShowMap: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            val shouldShowPrivateKeyChangeDialog = remember { mutableStateOf(false) }
            val shouldShowContactDeletionDialog = remember { mutableStateOf(false) }
            when {
                shouldShowPrivateKeyChangeDialog.value -> {
                    ChangeContactPrivateKeyDialog(contact = contact, trackedContactInfo = trackedContactInfo) {
                        shouldShowPrivateKeyChangeDialog.value = false
                    }
                }

                shouldShowContactDeletionDialog.value -> {
                    DeleteContactDialog(jsonDataStore = jsonDataStore, contact = contact) {
                        shouldShowContactDeletionDialog.value = false
                    }
                }
            }

            DeletableContactProfile(
                contact = contact
            ) {
                shouldShowContactDeletionDialog.value = true
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 8.dp)
            )

            // If the contact has at least more than one valid
            // phone number, then we show the phone number dropdown.
            // If not, we don't because it's useless
            if (Utils.getPhones(contact, true, true).size > 1) {
                PhoneNumberDropdown(
                    contact = contact,
                    currentChoice = Utils.formatNumber(trackedContactInfo.linkedPhoneNumber),
                    onPhoneChosen = { rawContact, phone ->
                        runBlocking {
                            jsonDataStore.updateTrackedContact(contact) { oldData ->
                                oldData.copy(linkedPhoneNumber = phone.normalizedNumber)
                            }
                        }
                    }
                )
            }

            Button(
                onClick = {
                    shouldShowPrivateKeyChangeDialog.value = true
                }
            ) {
                Text("Change password")
            }

            Text("Last time ping answer: ${trackedContactInfo.lastPingAnswer}")
            ActionButton(trackedContactInfo, Actions.PING) {
                Text("Ping phone")
            }

            ActionButton(trackedContactInfo, Actions.LOCATION) {
                Text("Request location")
            }

            Text("Battery charge: ${trackedContactInfo.lastRecordedBatteryCharge}%")
            ActionButton(trackedContactInfo, Actions.BATTERY) {
                Text("Get battery info")
            }

            Text("Last location answer: ${trackedContactInfo.geolocation}")
            Button(
                onClick = {
                    onShowMap()
                },
                enabled = trackedContactInfo.geolocation != null
            ) {
                Text(stringResource(R.string.show_map))
            }
        }
    }

    @Composable
    private fun ActionButton(
        trackingData: TrackingData,
        action: BaseAction<*, *>,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shape: Shape = ButtonDefaults.shape,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            onClick = {
                action.sendInstructionSMS(
                    this,
                    trackingData.linkedPhoneNumber!!,
                    trackingData.privateKey!!
                )
            },
            modifier = modifier,
            enabled = enabled && trackingData.linkedPhoneNumber != null,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource
        ) {
            content()
        }
    }

    @Composable
    private fun DeleteContactDialog(
        modifier: Modifier = Modifier,
        jsonDataStore: JsonDataStore,
        contact: Contact,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val resources = LocalResources.current

        AlertDialog(
            modifier = modifier,
            onDismissRequest = {
                onDismiss()
            },
            title = {
                Text(stringResource(R.string.confirmation_text))
            },
            text = {
                Text(stringResource(R.string.deletion_confirmation))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        this.isContactBeingDeleted = true

                        runBlocking {
                            if (jsonDataStore.removeTrackedContact(contact)) {
                                startActivity(Intent(this@UserTrackingActivity, TrackingActivity::class.java))
                            } else {
                                val deleteErrorMsg = resources.getString(R.string.delete_error, contact.displayNamePrimary)
                                Toast.makeText(context, deleteErrorMsg, Toast.LENGTH_SHORT).show()

                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {}
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    @Composable
    private fun UserLocationMap(
        modifier: Modifier = Modifier,
        trackingData: TrackingData,
        onHideMap: () -> Unit
    ) {
        if (trackingData.geolocation == null) {
            onHideMap()
        } else {
            UserLocationMapInner(modifier, trackingData)
            ElevatedButton(
                modifier = modifier
                    .padding(5.dp),
                onClick = onHideMap
            ) {
                Text(stringResource(R.string.go_back))
            }
        }
    }

    @Composable
    private fun ChangeContactPrivateKeyDialog(modifier: Modifier = Modifier, contact: Contact, trackedContactInfo: TrackingData, onDismiss: () -> Unit) {
        ChangeContactPrivateKeyDialog(modifier, contact, trackedContactInfo, onDismiss, onDismiss)
    }

    @Composable
    private fun ChangeContactPrivateKeyDialog(modifier: Modifier = Modifier, contact: Contact, trackedContactInfo: TrackingData, onDismiss: () -> Unit, onClose: () -> Unit = onDismiss) {
        val jsonDataStore = rememberJsonDatastore()

        val privateKeyState = rememberTextFieldState()
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        runBlocking {
                            jsonDataStore.updateTrackedContact(contact) {
                                trackedContactInfo.copy(privateKey = Utils.hashString("SHA-256", (privateKeyState.text as String).toByteArray()))
                            }
                        }

                        onClose()
                    },
                    enabled = Utils.isPasswordValid(privateKeyState.text)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                OutlinedSecureTextField(
                    privateKeyState,
                    label = { Text("User's private key") },
                    isError = !Utils.isPasswordValid(privateKeyState.text)
                )
            }
        )
    }

    @Composable
    private fun UserLocationMapInner(
        modifier: Modifier = Modifier,
        trackingData: TrackingData
    ) {
        val geolocation = trackingData.geolocation!!

        val camera =
            rememberCameraState(
                firstPosition =
                    CameraPosition(
                        target = Position(
                            latitude = 0.0,
                            longitude = 0.0
                        ),
                        zoom = 15.5
                    )
            )

        val oldGeolocation = remember { mutableStateOf(geolocation.copy()) }
        if (geolocation != oldGeolocation.value) {
            LaunchedEffect(Unit) {
                camera.animateTo(
                    finalPosition =
                        camera.position.copy(
                            target = Position(
                                latitude = geolocation.latitude,
                                longitude = geolocation.longitude
                            )
                        ),
                    duration = 1.5.seconds,
                )
            }

            oldGeolocation.value = geolocation.copy()
        }

        LaunchedEffect(Unit) {
            camera.animateTo(
                finalPosition =
                    camera.position.copy(
                        target = Position(
                            latitude = geolocation.latitude,
                            longitude = geolocation.longitude
                        )
                    ),
                duration = 3.seconds,
            )
        }

        MaplibreMap(
            modifier = modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            options =
                MapOptions(
                    ornamentOptions =
                        OrnamentOptions(
                            isScaleBarEnabled = false
                        )
                ),
            cameraState = camera
        ) {
            Marker(trackingData.geolocation)
        }
    }

    @Composable
    private fun Marker(geolocation: SerializableGeolocation) {
        val markerJson = """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [${geolocation.longitude},
${geolocation.latitude}]
      },
      "properties": {}
    }
  ]
}
        """.trimIndent()

        val markerSource = rememberGeoJsonSource(GeoJsonData.JsonString(markerJson))

        SymbolLayer(
            id = "marker-layer",
            source = markerSource,
            iconImage = image(painterResource(R.drawable.ic_location_icon)),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true)
        )
    }
}
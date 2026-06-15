package fr.geming400.localisationhelper.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import contacts.core.Contacts
import contacts.core.LookupQuery
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.actions.Actions
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.TrackingData
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.ContactProfile
import fr.geming400.localisationhelper.ui.components.PhoneNumberDropdown
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import kotlinx.coroutines.runBlocking
import org.osmdroid.util.GeoPoint

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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
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

                    return@LocalisationHelperTheme
                }

                val trackedContactInfo = jsonDataStore.getTrackedContact(trackedContacts, contact)

                snackbarHostState = remember { SnackbarHostState() }
                ActivitySelector(AppDestinations.TRACKING) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        }
                    ) { innerPadding ->
                        MainUserTrackingComponent(
                            innerPadding = innerPadding,
                            contact = contact,
                            trackedContactInfo = trackedContactInfo,
                            jsonDataStore = jsonDataStore
                        )
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
}

@Composable
fun localUserTrackingActivity(): UserTrackingActivity =
    LocalActivity.current as UserTrackingActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainUserTrackingComponent(
    innerPadding: PaddingValues,
    contact: Contact,
    trackedContactInfo: TrackingData,
    jsonDataStore: JsonDataStore = JsonDataStore(localUserTrackingActivity())
) {
    val activity = localUserTrackingActivity()
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(innerPadding),
    ) {
        ContactProfile(
            contact = contact
        )

        val openAlertDialog = remember { mutableStateOf(false) }

        when {
            openAlertDialog.value -> {
                DeleteContactDialog(jsonDataStore = jsonDataStore, contact = contact) {
                    openAlertDialog.value = false
                }
            }
        }

        Button(
            onClick = {
                openAlertDialog.value = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(stringResource(R.string.delete))
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

        Text("Last time ping answer: ${trackedContactInfo.lastPingAnswer}")

        Button(
            onClick = {
                if (trackedContactInfo.linkedPhoneNumber != null) {
                    Actions.PING.sendInstructionSMS(activity, trackedContactInfo.linkedPhoneNumber)
                } else {
                    Toast.makeText(activity, "linked phone number is null, can't do anything", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Ping phone")
        }

        Button(
            onClick = {
                if (trackedContactInfo.linkedPhoneNumber != null) {
                    Actions.LOCATION.sendInstructionSMS(activity, trackedContactInfo.linkedPhoneNumber)
                } else {
                    Toast.makeText(activity, "linked phone number is null, can't do anything", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Request location")
        }

        Text("Last time location answer: ${trackedContactInfo.geolocation?.lastTimeRecorded}")
        Button(
            onClick = {
                if (trackedContactInfo.geolocation != null) {
                    val intent = Intent(context, OSMActivity::class.java)
                    val bundle = Bundle()
                    bundle.putDouble("latitude", trackedContactInfo.geolocation.latitude)
                    bundle.putDouble("longitude", trackedContactInfo.geolocation.longitude)
                    bundle.putDouble("zoom", 18.5)
                    bundle.putParcelableArray("markers", arrayOf(
                        GeoPoint(trackedContactInfo.geolocation.latitude, trackedContactInfo.geolocation.longitude)
                    ))

                    intent.putExtras(bundle)
                    activity.startActivity(intent)
                } else {
                    Toast.makeText(activity, "Phone's geolocation was never recorded", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = trackedContactInfo.geolocation != null
        ) {
            Text("Show map")
        }
    }
}

@Composable
private fun DeleteContactDialog(
    modifier: Modifier = Modifier,
    jsonDataStore: JsonDataStore,
    contact: Contact,
    onDismiss: () -> Unit
) {
    val activity = localUserTrackingActivity()
    rememberCoroutineScope()
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
                    activity.isContactBeingDeleted = true

                    runBlocking {
                        if (jsonDataStore.removeTrackedContact(contact)) {
                            activity.startActivity(Intent(activity, TrackingActivity::class.java))
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
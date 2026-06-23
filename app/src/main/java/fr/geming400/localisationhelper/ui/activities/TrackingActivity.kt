package fr.geming400.localisationhelper.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import contacts.core.Contacts
import contacts.core.LookupQuery
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.datastore.JsonDataStore
import fr.geming400.localisationhelper.datastore.LocalisationHelperData
import fr.geming400.localisationhelper.datastore.dataStore
import fr.geming400.localisationhelper.ui.components.ActivitySelector
import fr.geming400.localisationhelper.ui.components.AppDestinations
import fr.geming400.localisationhelper.ui.components.ContactsList
import fr.geming400.localisationhelper.ui.components.LoadingCircle
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import kotlinx.coroutines.runBlocking

class TrackingActivity : ComponentActivity() {
    val contactPickerLauncher: ActivityResultLauncher<Void?> =
        registerForActivityResult(
            ActivityResultContracts.PickContact()
        ) { result ->
            var jsonDataStore = JsonDataStore(this)

            // when result == null, the user chose no contacts
            if (result != null) {
                val contactId = result.toString().substringAfterLast("/").toLong()
                val lookupKey = result.toString().substringAfter("lookup/").substringBefore("/")
                var lookupKeyWithId = LookupQuery.LookupKeyWithId(lookupKey, contactId)

                val contact = Contacts(this)
                    .lookupQuery()
                    .whereLookupKeyWithIdMatches(lookupKeyWithId)
                    .find()
                    .first()

                if (Utils.hasAnyValidNumber(contact)) {
                    runBlocking {
                        jsonDataStore.addTrackedContact(lookupKeyWithId)
                        jsonDataStore.updateTrackedContact(contact) {
                            it.copy(linkedPhoneNumber = Utils.getFirstValidPhone(contact)?.normalizedNumber)
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.has_no_valid_numbers_error, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val jsonDataStore = remember(this) { JsonDataStore(this) }

            val trackedContacts by jsonDataStore.trackedContactsFlow()
                .collectAsState(initial = emptyList(), coroutineScope.coroutineContext)

            val appData by this.dataStore.data
                .collectAsState(initial = null, coroutineScope.coroutineContext)

            LocalisationHelperTheme {
                if (appData == null) {
                    LoadingCircle()
                } else {
                    ActivitySelector(AppDestinations.TRACKING) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            floatingActionButton = {
                                if (appData!!.passwordHash != null)
                                    ContactAdderButton()
                            }
                        ) { innerPadding ->
                            if (appData!!.passwordHash != null) {
                                ContactsList(
                                    modifier = Modifier.padding(innerPadding),
                                    contactsFilter = trackedContacts.map { it.lookupKeyWithId },
                                    onContactClick = { contact ->
                                        val intent = Intent(this, UserTrackingActivity::class.java)
                                        val bundle = Bundle()
                                        bundle.putString("lookupKey", contact.lookupKey)
                                        bundle.putLong("contactID", contact.id)
                                        intent.putExtras(bundle)

                                        this.startActivity(intent)
                                    }
                                ) {
                                    MustAddContactsText(Modifier.padding(innerPadding), appData!!)
                                }
                            } else {
                                MustAddContactsText(Modifier.padding(innerPadding), appData!!)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MustAddContactsText(modifier: Modifier = Modifier, appData: LocalisationHelperData) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val text =
            if (appData.passwordHash == null) R.string.must_set_password else R.string.add_contacts
        val fontWeight =
            if (appData.passwordHash == null) FontWeight.Bold else null

        Column() {
            Text(
                stringResource(text),
                fontSize = 4.em,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center
            )
        }
    }
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
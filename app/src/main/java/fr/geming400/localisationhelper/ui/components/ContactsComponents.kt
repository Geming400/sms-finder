package fr.geming400.localisationhelper.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import contacts.core.Contacts
import contacts.core.ContactsFields
import contacts.core.LookupQuery
import contacts.core.asc
import contacts.core.desc
import contacts.core.entities.Contact
import contacts.core.entities.Phone
import contacts.core.entities.RawContact
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.utils.Utils
import fr.geming400.localisationhelper.set

@Composable
fun ContactProfile(modifier: Modifier = Modifier, contact: Contact, onClick: () -> Unit) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        ContactProfileInner(contact)
    }
}

@Composable
fun ContactProfile(modifier: Modifier = Modifier, contact: Contact) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ContactProfileInner(contact)
    }
}

@Composable
private fun ContactProfileInner(contact: Contact) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = contact.displayNamePrimary ?: stringResource(R.string.unknown_contact)
        val imageModifier = Modifier
            .clip(CircleShape)
            .size(75.dp)

        if (contact.photoThumbnailUri == null) {
            Image(
                painter = painterResource(R.drawable.ic_person),
                // contentDescription = "The default photo of the contact $displayName",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } else {
            AsyncImage(
                model = contact.photoThumbnailUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        }

        Text(
            displayName,
            modifier = Modifier
                .padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun ContactsList(
    modifier: Modifier = Modifier,
    contactsFilter: List<LookupQuery.LookupKeyWithId>,
    onContactClick: (contact: Contact) -> Unit = {},
    whenNoContacts: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    val result = Contacts(context)
        .lookupQuery()
        .include { Phone.all + DataId + Contact.DisplayNamePrimary + Contact.PhotoThumbnailUri + Contact.LookupKey }
        .orderBy(
            ContactsFields.Options.Starred.desc(),
            ContactsFields.DisplayNamePrimary.asc()
        )
        .whereLookupKeyWithIdMatches(contactsFilter)
        .find()

    if (result.isEmpty()) {
        whenNoContacts()
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(result) { contact ->
                ContactProfile(
                    contact = contact,
                    onClick = { onContactClick(contact) }
                )
            }
        }
    }
}

/**
 * Gets the first phone number of a given [Contact]
 */
private fun getFirstPhoneNumber(contact: Contact): String? {
    val rawContact = contact.rawContacts.firstOrNull()
    val phones = rawContact?.phones?.firstOrNull()
    return phones?.normalizedNumber
}

@Composable
fun PhoneNumberDropdown(
    modifier: Modifier = Modifier,
    contact: Contact,
    currentChoice: String? = null,
    onPhoneChosen: (rawContact: RawContact, phone: Phone) -> Unit
) {
    var mTextFieldSize by remember { mutableStateOf(Size.Zero)}
    var expanded by remember { mutableStateOf(false) }

    val textValue: TextFieldState = if (currentChoice == null) {
        rememberTextFieldState(
            Utils.formatNumber(
                getFirstPhoneNumber(contact),
                stringResource(R.string.no_viable_phone_number_error)
            )
        )
    } else {
        rememberTextFieldState(currentChoice)
    }

    Box(
        modifier = modifier
    ) {
        OutlinedTextField(
            textValue,
            modifier = Modifier
                .onGloballyPositioned { mTextFieldSize = it.size.toSize() },
            label = {
                Text(stringResource(R.string.linked_phone_number))
            },
            readOnly = true,
            trailingIcon = {
                val icon = if (expanded) {
                    painterResource(R.drawable.ic_expand_less)
                } else {
                    painterResource(R.drawable.ic_expand_more)
                }

                Icon(
                    painter = icon,
                    contentDescription = stringResource(R.string.content_description_click_to_expand),
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                )
            },
        )

        val renderedPhoneNumbers = mutableListOf<String>()

        DropdownMenu(
            expanded = expanded,
            modifier = Modifier
                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() } ),
            onDismissRequest = {
                expanded = false
            }
        ) {
            contact.rawContacts.forEach { rawContact ->
                for (phone: Phone in rawContact.phones) {
                    if (!Utils.isNumberValid(phone))
                        continue

                    if (phone.normalizedNumber in renderedPhoneNumbers)
                        continue

                    renderedPhoneNumbers.add(phone.normalizedNumber ?: "")

                    val formattedNum = Utils.formatNumber(phone)
                    Column() {
                        DropdownMenuItem(
                            text = {
                                Text(formattedNum)
                            },
                            onClick = {
                                onPhoneChosen(rawContact, phone)
                                expanded = false

                                textValue.set(formattedNum)
                            }
                        )
                    }
                }
            }
        }
    }
}
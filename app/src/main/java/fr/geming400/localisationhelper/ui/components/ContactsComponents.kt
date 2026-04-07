package fr.geming400.localisationhelper.ui.components

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import contacts.core.Contacts
import contacts.core.ContactsFields
import contacts.core.asc
import contacts.core.desc
import contacts.core.entities.Contact
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.activities.UserTrackingActivity

@Composable
fun ContactProfile(contact: Contact) {
    val activity = LocalActivity.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        onClick = {
            activity?.startActivity(Intent(activity.applicationContext, UserTrackingActivity::class.java))
        }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayName = contact.displayNamePrimary ?: "Unknown contact"
            val imageModifier = Modifier
                .clip(CircleShape)
                .size(75.dp)

            if (contact.photoThumbnailUri == null) {
                Image(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = "The default photo of the contact $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                )
            } else {
                AsyncImage(
                    model = contact.photoThumbnailUri,
                    contentDescription = "The photo of the contact $displayName",
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
}

@Composable
fun ContactsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current;

    val projection = arrayOf(
        ContactsContract.Profile._ID,
        ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
        ContactsContract.Profile.LOOKUP_KEY,
        ContactsContract.Profile.PHOTO_THUMBNAIL_URI
    )

    val result = Contacts(context)
        .broadQuery()
        .include { Phone.all + DataId + Contact.DisplayNamePrimary + Contact.PhotoThumbnailUri }
        .orderBy(
            ContactsFields.Options.Starred.desc(),
            ContactsFields.DisplayNamePrimary.asc()
        )
        .find()



    LazyColumn(
        modifier = modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (contact in result) {
            item {
                ContactProfile(contact)
            }
        }
    }
}
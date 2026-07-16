package fr.geming400.localisationhelper.datastore

import kotlinx.serialization.Serializable

@Serializable
data class ExtraInfo(
    val isMobileDataEnabled: Boolean,
    // dnd = Do Not Disturb
    val isDndEnabled: Boolean
)
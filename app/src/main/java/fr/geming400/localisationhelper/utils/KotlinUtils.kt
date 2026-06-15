package fr.geming400.localisationhelper.utils

import androidx.compose.foundation.text.input.TextFieldState

/**
 * Sets the text of this [TextFieldState] to `text`
 * @param text the text to set
 */
fun TextFieldState.set(text: String) {
    edit {
        replace(0, length, text)
    }
}
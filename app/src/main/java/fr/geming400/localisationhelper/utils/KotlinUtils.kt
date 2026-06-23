package fr.geming400.localisationhelper.utils

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Sets the text of this [TextFieldState] to `text`
 * @param text the text to set
 */
fun TextFieldState.set(text: String) =
    edit {
        replace(0, length, text)
    }

fun Modifier.centerHorizontally(fraction: Float = 1f, unbounded: Boolean = false): Modifier =
    this
        .fillMaxWidth(fraction)
        .wrapContentWidth(Alignment.CenterHorizontally, unbounded)

fun Modifier.centerVertically(fraction: Float = 1f, unbounded: Boolean = false): Modifier =
    this
        .fillMaxHeight(fraction)
        .wrapContentHeight(Alignment.CenterVertically, unbounded)
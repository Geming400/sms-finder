package fr.geming400.localisationhelper.utils

import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import fr.geming400.localisationhelper.R

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

@IntRange(from = 0, to = 1)
fun Boolean.toInt(): Int =
    if (this) 1 else 0

fun Int.toBool(): Boolean =
    this == 1

fun String.toBoolFromInt(): Boolean =
    this.toInt().toBool()

@Composable
@ReadOnlyComposable
fun nullableStringResource(@StringRes id: Int, vararg formatArgs: Any?, default: Any = "null"): String {
    val nullsafeFormatArgs = formatArgs.map {
        it ?: default
    }

    return LocalResources.current.getString(id, *nullsafeFormatArgs.toTypedArray())
}

@Composable
@ReadOnlyComposable
fun getYesOrNo(bool: Boolean?): String {
    return if (bool == true)
        stringResource(R.string.yes)
    else if (bool == false)
        stringResource(R.string.no)
    else
        stringResource(R.string.unknown)
}
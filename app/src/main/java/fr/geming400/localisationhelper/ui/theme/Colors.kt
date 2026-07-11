package fr.geming400.localisationhelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

class Colors {
    companion object {
        @Composable
        @ReadOnlyComposable
        fun green() =
            if (isSystemInDarkTheme()) Green40 else Green80
    }
}
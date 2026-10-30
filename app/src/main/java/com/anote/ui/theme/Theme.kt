package com.anote.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4E2D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3DCCB),
    onPrimaryContainer = Color(0xFF2C1608),
    secondary = Color(0xFF5C6B4E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE8D2),
    onSecondaryContainer = Color(0xFF1A2414),
    tertiary = Color(0xFF6E5A3C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F0E7),
    onBackground = Color(0xFF1E1B17),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF1E1B17),
    surfaceVariant = Color(0xFFEFE4D6),
    onSurfaceVariant = Color(0xFF51453A),
    outline = Color(0xFFD4C4B4),
    error = Color(0xFFA33B32),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7C3A8),
    onPrimary = Color(0xFF3F2614),
    primaryContainer = Color(0xFF5C3B26),
    onPrimaryContainer = Color(0xFFF3DCCB),
    secondary = Color(0xFFC5D4B6),
    onSecondary = Color(0xFF2A3622),
    secondaryContainer = Color(0xFF3E4C34),
    onSecondaryContainer = Color(0xFFDEE8D2),
    tertiary = Color(0xFFE4C9A4),
    onTertiary = Color(0xFF3D2E16),
    background = Color(0xFF14110E),
    onBackground = Color(0xFFF3EBE3),
    surface = Color(0xFF1E1A16),
    onSurface = Color(0xFFF3EBE3),
    surfaceVariant = Color(0xFF3A322B),
    onSurfaceVariant = Color(0xFFD9CBBC),
    outline = Color(0xFF5A4E44),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun AnoteTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun anoteTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
)

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

package app.orionmd.management.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.orionmd.management.data.entity.ThemeMode

// Street-art / rusty-metal palette pulled from the app icon
val RentalsRust = Color(0xFF8B4A2B)
val RentalsTeal = Color(0xFF3FB6C4)
val RentalsOrange = Color(0xFFE8863A)
val RentalsPurple = Color(0xFF7C5CBF)
val RentalsCharcoal = Color(0xFF1A1A1C)
val RentalsCharcoalDark = Color(0xFF111113)
val RentalsOffWhite = Color(0xFFF4F1EA)
val RentalsBgLight = Color(0xFFF6F4EF)
// A neutral gray rather than near-black - the original near-black background read as "too dark".
val RentalsBgDark = Color(0xFF3A3A3C)
// Material Gray theme - a flat, neutral gray palette (no warm/cool cast), distinct from both
// the warm Day theme and the near-black Night theme.
val MaterialGraySurface = Color(0xFF6C6C70)
val MaterialGrayBackground = Color(0xFF5A5A5E)

private val DayColors = lightColorScheme(
    primary = RentalsOrange,
    onPrimary = Color.White,
    secondary = RentalsTeal,
    onSecondary = Color.White,
    tertiary = RentalsPurple,
    background = RentalsBgLight,
    onBackground = Color(0xFF201C18),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF201C18),
    surfaceVariant = Color(0xFFEDE7DC),
    error = Color(0xFFB3261E)
)

private val NightColors = darkColorScheme(
    primary = RentalsOrange,
    onPrimary = Color(0xFF1A1A1C),
    secondary = RentalsTeal,
    onSecondary = Color(0xFF00202A),
    tertiary = RentalsPurple,
    background = RentalsBgDark,
    onBackground = RentalsOffWhite,
    surface = Color(0xFF454547),
    onSurface = RentalsOffWhite,
    surfaceVariant = Color(0xFF505052),
    error = Color(0xFFF2B8B5)
)

private val MaterialGrayColors = darkColorScheme(
    primary = RentalsTeal,
    onPrimary = Color(0xFF00202A),
    secondary = RentalsOrange,
    onSecondary = Color(0xFF2B1600),
    tertiary = RentalsPurple,
    background = MaterialGrayBackground,
    onBackground = Color(0xFFF0F0F0),
    surface = MaterialGraySurface,
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF7A7A7E),
    error = Color(0xFFF2B8B5)
)

/**
 * Applies one of the three named themes (Settings > Theme): Day (light), Night (dark), or
 * Material Gray (a flat neutral gray, distinct from both). [darkStatusBarIcons] controls whether
 * status/navigation bar icons should be drawn dark (only true for the light Day theme).
 */
@Composable
fun RentalsTheme(
    themeMode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.NIGHT else ThemeMode.DAY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DAY -> DayColors
        ThemeMode.NIGHT -> NightColors
        ThemeMode.GRAY -> MaterialGrayColors
    }
    val isLight = themeMode == ThemeMode.DAY
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RentalsTypography,
        content = content
    )
}

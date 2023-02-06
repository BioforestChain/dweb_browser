package info.bagen.libappmgr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorPalette = Colors(
    primary = Color(0xFF757575),
    primaryVariant = Color(0xFF666666),
    secondary = Color(0xFFBFBFBF),
    secondaryVariant = Color(0xFFBFBFBF),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFDBDBDB),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFDBDBDB),
    onError = Color(0xFFDBDBDB),
    isLight = false
)

val LightColorPalette = Colors(
    primary = Color(0xFFF2F3F5),
    primaryVariant = Color(0xFFCCCCCC),
    secondary = Color(0xFFBFBFBF),
    secondaryVariant = Color(0xFFBFBFBF),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB00020),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF8A8A8A),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onError = Color(0xFF000000),
    isLight = true
)

@Composable
fun AppMgrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

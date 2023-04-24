package info.bagen.dwebbrowser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RustApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit
) {
  val colors = if (darkTheme) {
    DarkColorPalette
  } else {
    LightColorPalette
  }

  MaterialTheme(
    colorScheme = colors, typography = Typography, shapes = Shapes, content = content
  )
}

val DarkColorPalette = darkColorScheme(
  background           = PaletteTokens.Neutral10       ,
  error                = PaletteTokens.Error80         ,
  errorContainer       = PaletteTokens.Error30         ,
  inverseOnSurface     = PaletteTokens.Neutral20       ,
  inversePrimary       = PaletteTokens.Primary40       ,
  inverseSurface       = PaletteTokens.Neutral90       ,
  onBackground         = PaletteTokens.Neutral90       ,
  onError              = PaletteTokens.Error20         ,
  onErrorContainer     = PaletteTokens.Error90         ,
  onPrimary            = PaletteTokens.Primary20       ,
  onPrimaryContainer   = PaletteTokens.Primary90       ,
  onSecondary          = PaletteTokens.Secondary20     ,
  onSecondaryContainer = PaletteTokens.Secondary90     ,
  onSurface            = PaletteTokens.Neutral90       ,
  onSurfaceVariant     = PaletteTokens.NeutralVariant80,
  onTertiary           = PaletteTokens.Tertiary20      ,
  onTertiaryContainer  = PaletteTokens.Tertiary90      ,
  outline              = PaletteTokens.NeutralVariant60,
  outlineVariant       = PaletteTokens.NeutralVariant30,
  primary              = PaletteTokens.Primary80       ,
  primaryContainer     = PaletteTokens.Primary30       ,
  scrim                = PaletteTokens.Neutral0        ,
  secondary            = PaletteTokens.Secondary80     ,
  secondaryContainer   = PaletteTokens.Secondary30     ,
  surface              = PaletteTokens.Neutral10       ,
  surfaceTint          = PaletteTokens.Primary80       ,
  surfaceVariant       = PaletteTokens.NeutralVariant30,
  tertiary             = PaletteTokens.Tertiary80      ,
  tertiaryContainer    = PaletteTokens.Tertiary30      ,
)

val LightColorPalette = lightColorScheme(
  background           = PaletteTokens.Neutral99       ,
  error                = PaletteTokens.Error40         ,
  errorContainer       = PaletteTokens.Error90         ,
  inverseOnSurface     = PaletteTokens.Neutral95       ,
  inversePrimary       = PaletteTokens.Primary80       ,
  inverseSurface       = PaletteTokens.Neutral20       ,
  onBackground         = PaletteTokens.Neutral10       ,
  onError              = PaletteTokens.Error100        ,
  onErrorContainer     = PaletteTokens.Error10         ,
  onPrimary            = PaletteTokens.Primary100      ,
  onPrimaryContainer   = PaletteTokens.Primary10       ,
  onSecondary          = PaletteTokens.Secondary100    ,
  onSecondaryContainer = PaletteTokens.Secondary10     ,
  onSurface            = PaletteTokens.Neutral10       ,
  onSurfaceVariant     = PaletteTokens.NeutralVariant30,
  onTertiary           = PaletteTokens.Tertiary100     ,
  onTertiaryContainer  = PaletteTokens.Tertiary10      ,
  outline              = PaletteTokens.NeutralVariant50,
  outlineVariant       = PaletteTokens.NeutralVariant80,
  primary              = PaletteTokens.Primary40       ,
  primaryContainer     = PaletteTokens.Primary90       ,
  scrim                = PaletteTokens.Neutral0        ,
  secondary            = PaletteTokens.Secondary40     ,
  secondaryContainer   = PaletteTokens.Secondary90     ,
  surface              = PaletteTokens.Neutral99       ,
  surfaceTint          = PaletteTokens.Primary40       ,
  surfaceVariant       = PaletteTokens.NeutralVariant90,
  tertiary             = PaletteTokens.Tertiary40      ,
  tertiaryContainer    = PaletteTokens.Tertiary90      ,
)
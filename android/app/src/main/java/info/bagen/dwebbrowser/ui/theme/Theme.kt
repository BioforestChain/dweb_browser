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
  primary              = PaletteTokens.Primary80       ,
  onPrimary            = PaletteTokens.Primary20       ,
  primaryContainer     = PaletteTokens.Primary30       ,
  onPrimaryContainer   = PaletteTokens.Primary90       ,
  inversePrimary       = PaletteTokens.Primary40       , // 上面都是关于 Primary 的
  secondary            = PaletteTokens.Secondary80     ,
  onSecondary          = PaletteTokens.Secondary20     ,
  secondaryContainer   = PaletteTokens.Secondary30     ,
  onSecondaryContainer = PaletteTokens.Secondary90     , // 上面都是关于 Secondary 的
  tertiary             = PaletteTokens.Tertiary80      ,
  onTertiary           = PaletteTokens.Tertiary20      ,
  tertiaryContainer    = PaletteTokens.Tertiary30      ,
  onTertiaryContainer  = PaletteTokens.Tertiary90      , // 上面都是关于 Tertiary 的
  background           = PaletteTokens.Neutral10       ,
  onBackground         = PaletteTokens.Neutral90       , // 上面都是关于 Background 的
  surface              = PaletteTokens.Neutral0        ,
  onSurface            = PaletteTokens.Neutral100      ,
  surfaceVariant       = PaletteTokens.NeutralVariant30,
  onSurfaceVariant     = PaletteTokens.NeutralVariant80,
  surfaceTint          = PaletteTokens.Primary80       ,
  inverseSurface       = PaletteTokens.Neutral90       ,
  inverseOnSurface     = PaletteTokens.Neutral20       , // 上面都是关于 Surface 的
  error                = PaletteTokens.Error80         ,
  onError              = PaletteTokens.Error20         ,
  errorContainer       = PaletteTokens.Error30         ,
  onErrorContainer     = PaletteTokens.Error90         , // 上面都是关于 Error 的
  outline              = PaletteTokens.NeutralVariant60,
  outlineVariant       = PaletteTokens.NeutralVariant30,
  scrim                = PaletteTokens.Neutral0        ,
)

val LightColorPalette = lightColorScheme(
  primary              = PaletteTokens.Primary40       ,
  onPrimary            = PaletteTokens.Primary100      ,
  primaryContainer     = PaletteTokens.Primary90       ,
  onPrimaryContainer   = PaletteTokens.Primary10       ,
  inversePrimary       = PaletteTokens.Primary80       , // 上面都是关于 Primary 的
  secondary            = PaletteTokens.Secondary40     ,
  onSecondary          = PaletteTokens.Secondary100    ,
  secondaryContainer   = PaletteTokens.Secondary90     ,
  onSecondaryContainer = PaletteTokens.Secondary10     , // 上面都是关于 Secondary 的
  tertiary             = PaletteTokens.Tertiary40      ,
  onTertiary           = PaletteTokens.Tertiary100     ,
  tertiaryContainer    = PaletteTokens.Tertiary90      ,
  onTertiaryContainer  = PaletteTokens.Tertiary10      , // 上面都是关于 Tertiary 的
  background           = PaletteTokens.Neutral99       ,
  onBackground         = PaletteTokens.Neutral10       , // 上面都是关于 Background 的
  surface              = PaletteTokens.Neutral100      ,
  onSurface            = PaletteTokens.Neutral0        ,
  surfaceVariant       = PaletteTokens.NeutralVariant90,
  onSurfaceVariant     = PaletteTokens.NeutralVariant30,
  surfaceTint          = PaletteTokens.Primary40       ,
  inverseSurface       = PaletteTokens.Neutral20       ,
  inverseOnSurface     = PaletteTokens.Neutral95       , // 上面都是关于 Surface 的
  error                = PaletteTokens.Error40         ,
  onError              = PaletteTokens.Error100        ,
  errorContainer       = PaletteTokens.Error90         ,
  onErrorContainer     = PaletteTokens.Error10         , // 上面都是关于 Error 的
  outline              = PaletteTokens.NeutralVariant50,
  outlineVariant       = PaletteTokens.NeutralVariant80,
  scrim                = PaletteTokens.Neutral0        ,
)
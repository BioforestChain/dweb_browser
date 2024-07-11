package org.dweb_browser.helper.platform.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.dweb_browser.helper.compose.compositionChainOf

data class Colorful(
  val Red: ColorPalettes = defaultRed,
  val Pink: ColorPalettes = defaultPink,
  val Purple: ColorPalettes = defaultPurple,
  val DeepPurple: ColorPalettes = defaultDeepPurple,
  val Indigo: ColorPalettes = defaultIndigo,
  val Blue: ColorPalettes = defaultBlue,
  val LightBlue: ColorPalettes = defaultLightBlue,
  val Cyan: ColorPalettes = defaultCyan,
  val Teal: ColorPalettes = defaultTeal,
  val Green: ColorPalettes = defaultGreen,
  val LightGreen: ColorPalettes = defaultLightGreen,
  val Lime: ColorPalettes = defaultLime,
  val Yellow: ColorPalettes = defaultYellow,
  val Amber: ColorPalettes = defaultAmber,
  val Orange: ColorPalettes = defaultOrange,
  val DeepOrange: ColorPalettes = defaultDeepOrange,
  val Brown: ColorPalettes = defaultBrown,
  val Gray: ColorPalettes = defaultGray,
  val BlueGray: ColorPalettes = defaultBlueGray,
) {
  companion object {
    val default by lazy { Colorful() }
    val defaultRed = ColorPalettes(
      Shade_50 = Color(0xFFFFEBEE),
      Shade_100 = Color(0xFFFFCDD2),
      Shade_200 = Color(0xFFEF9A9A),
      Shade_300 = Color(0xFFE57373),
      Shade_400 = Color(0xFFEF5350),
      Shade_500 = Color(0xFFF44336),
      Shade_600 = Color(0xFFE53935),
      Shade_700 = Color(0xFFD32F2F),
      Shade_800 = Color(0xFFC62828),
      Shade_900 = Color(0xFFB71C1C),
    )
    val defaultPink = ColorPalettes(
      Shade_50 = Color(0xFFFCE4EC),
      Shade_100 = Color(0xFFF8BBD0),
      Shade_200 = Color(0xFFF48FB1),
      Shade_300 = Color(0xFFF06292),
      Shade_400 = Color(0xFFEC407A),
      Shade_500 = Color(0xFFE91E63),
      Shade_600 = Color(0xFFD81B60),
      Shade_700 = Color(0xFFC2185B),
      Shade_800 = Color(0xFFAD1457),
      Shade_900 = Color(0xFF880E4F),
    )
    val defaultPurple = ColorPalettes(
      Shade_50 = Color(0xFFF3E5F5),
      Shade_100 = Color(0xFFE1BEE7),
      Shade_200 = Color(0xFFCE93D8),
      Shade_300 = Color(0xFFBA68C8),
      Shade_400 = Color(0xFFAB47BC),
      Shade_500 = Color(0xFF9C27B0),
      Shade_600 = Color(0xFF8E24AA),
      Shade_700 = Color(0xFF7B1FA2),
      Shade_800 = Color(0xFF6A1B9A),
      Shade_900 = Color(0xFF4A148C),
    )
    val defaultDeepPurple = ColorPalettes(
      Shade_50 = Color(0xFFEDE7F6),
      Shade_100 = Color(0xFFD1C4E9),
      Shade_200 = Color(0xFFB39DDB),
      Shade_300 = Color(0xFF9575CD),
      Shade_400 = Color(0xFF7E57C2),
      Shade_500 = Color(0xFF673AB7),
      Shade_600 = Color(0xFF5E35B1),
      Shade_700 = Color(0xFF512DA8),
      Shade_800 = Color(0xFF4527A0),
      Shade_900 = Color(0xFF311B92),
    )
    val defaultIndigo = ColorPalettes(
      Shade_50 = Color(0xFFE8EAF6),
      Shade_100 = Color(0xFFC5CAE9),
      Shade_200 = Color(0xFF9FA8DA),
      Shade_300 = Color(0xFF7986CB),
      Shade_400 = Color(0xFF5C6BC0),
      Shade_500 = Color(0xFF3F51B5),
      Shade_600 = Color(0xFF3949AB),
      Shade_700 = Color(0xFF303F9F),
      Shade_800 = Color(0xFF283593),
      Shade_900 = Color(0xFF1A237E),
    )
    val defaultBlue = ColorPalettes(
      Shade_50 = Color(0xFFE3F2FD),
      Shade_100 = Color(0xFFBBDEFB),
      Shade_200 = Color(0xFF90CAF9),
      Shade_300 = Color(0xFF64B5F6),
      Shade_400 = Color(0xFF42A5F5),
      Shade_500 = Color(0xFF2196F3),
      Shade_600 = Color(0xFF1E88E5),
      Shade_700 = Color(0xFF1976D2),
      Shade_800 = Color(0xFF1565C0),
      Shade_900 = Color(0xFF0D47A1),
    )
    val defaultLightBlue = ColorPalettes(
      Shade_50 = Color(0xFFE1F5FE),
      Shade_100 = Color(0xFFB3E5FC),
      Shade_200 = Color(0xFF81D4FA),
      Shade_300 = Color(0xFF4FC3F7),
      Shade_400 = Color(0xFF29B6F6),
      Shade_500 = Color(0xFF03A9F4),
      Shade_600 = Color(0xFF039BE5),
      Shade_700 = Color(0xFF0288D1),
      Shade_800 = Color(0xFF0277BD),
      Shade_900 = Color(0xFF01579B),
    )
    val defaultCyan = ColorPalettes(
      Shade_50 = Color(0xFFE0F7FA),
      Shade_100 = Color(0xFFB2EBF2),
      Shade_200 = Color(0xFF80DEEA),
      Shade_300 = Color(0xFF4DD0E1),
      Shade_400 = Color(0xFF26C6DA),
      Shade_500 = Color(0xFF00BCD4),
      Shade_600 = Color(0xFF00ACC1),
      Shade_700 = Color(0xFF0097A7),
      Shade_800 = Color(0xFF00838F),
      Shade_900 = Color(0xFF006064),
    )
    val defaultTeal = ColorPalettes(
      Shade_50 = Color(0xFFE0F2F1),
      Shade_100 = Color(0xFFB2DFDB),
      Shade_200 = Color(0xFF80CBC4),
      Shade_300 = Color(0xFF4DB6AC),
      Shade_400 = Color(0xFF26A69A),
      Shade_500 = Color(0xFF009688),
      Shade_600 = Color(0xFF00897B),
      Shade_700 = Color(0xFF00796B),
      Shade_800 = Color(0xFF00695C),
      Shade_900 = Color(0xFF004D40),
    )
    val defaultGreen = ColorPalettes(
      Shade_50 = Color(0xFFE8F5E9),
      Shade_100 = Color(0xFFC8E6C9),
      Shade_200 = Color(0xFFA5D6A7),
      Shade_300 = Color(0xFF81C784),
      Shade_400 = Color(0xFF66BB6A),
      Shade_500 = Color(0xFF4CAF50),
      Shade_600 = Color(0xFF43A047),
      Shade_700 = Color(0xFF388E3C),
      Shade_800 = Color(0xFF2E7D32),
      Shade_900 = Color(0xFF1B5E20),
    )
    val defaultLightGreen = ColorPalettes(
      Shade_50 = Color(0xFFF1F8E9),
      Shade_100 = Color(0xFFDCEDC8),
      Shade_200 = Color(0xFFC5E1A5),
      Shade_300 = Color(0xFFAED581),
      Shade_400 = Color(0xFF9CCC65),
      Shade_500 = Color(0xFF8BC34A),
      Shade_600 = Color(0xFF7CB342),
      Shade_700 = Color(0xFF689F38),
      Shade_800 = Color(0xFF558B2F),
      Shade_900 = Color(0xFF33691E),
    )
    val defaultLime = ColorPalettes(
      Shade_50 = Color(0xFFF9FBE7),
      Shade_100 = Color(0xFFF0F4C3),
      Shade_200 = Color(0xFFE6EE9C),
      Shade_300 = Color(0xFFDCE775),
      Shade_400 = Color(0xFFD4E157),
      Shade_500 = Color(0xFFCDDC39),
      Shade_600 = Color(0xFFC0CA33),
      Shade_700 = Color(0xFFAFB42B),
      Shade_800 = Color(0xFF9E9D24),
      Shade_900 = Color(0xFF827717),
    )
    val defaultYellow = ColorPalettes(
      Shade_50 = Color(0xFFFFFDE7),
      Shade_100 = Color(0xFFFFF9C4),
      Shade_200 = Color(0xFFFFF59D),
      Shade_300 = Color(0xFFFFF176),
      Shade_400 = Color(0xFFFFEE58),
      Shade_500 = Color(0xFFFFEB3B),
      Shade_600 = Color(0xFFFDD835),
      Shade_700 = Color(0xFFFBC02D),
      Shade_800 = Color(0xFFF9A825),
      Shade_900 = Color(0xFFF57F17),
    )
    val defaultAmber = ColorPalettes(
      Shade_50 = Color(0xFFFFF8E1),
      Shade_100 = Color(0xFFFFECB3),
      Shade_200 = Color(0xFFFFE082),
      Shade_300 = Color(0xFFFFD54F),
      Shade_400 = Color(0xFFFFCA28),
      Shade_500 = Color(0xFFFFC107),
      Shade_600 = Color(0xFFFFB300),
      Shade_700 = Color(0xFFFFA000),
      Shade_800 = Color(0xFFFF8F00),
      Shade_900 = Color(0xFFFF6F00),
    )
    val defaultOrange = ColorPalettes(
      Shade_50 = Color(0xFFFFF3E0),
      Shade_100 = Color(0xFFFFE0B2),
      Shade_200 = Color(0xFFFFCC80),
      Shade_300 = Color(0xFFFFB74D),
      Shade_400 = Color(0xFFFFA726),
      Shade_500 = Color(0xFFFF9800),
      Shade_600 = Color(0xFFFB8C00),
      Shade_700 = Color(0xFFF57C00),
      Shade_800 = Color(0xFFEF6C00),
      Shade_900 = Color(0xFFE65100),
    )
    val defaultDeepOrange = ColorPalettes(
      Shade_50 = Color(0xFFFBE9E7),
      Shade_100 = Color(0xFFFFCCBC),
      Shade_200 = Color(0xFFFFAB91),
      Shade_300 = Color(0xFFFF8A65),
      Shade_400 = Color(0xFFFF7043),
      Shade_500 = Color(0xFFFF5722),
      Shade_600 = Color(0xFFF4511E),
      Shade_700 = Color(0xFFE64A19),
      Shade_800 = Color(0xFFD84315),
      Shade_900 = Color(0xFFBF360C),
    )
    val defaultBrown = ColorPalettes(
      Shade_50 = Color(0xFFEFEBE9),
      Shade_100 = Color(0xFFD7CCC8),
      Shade_200 = Color(0xFFBCAAA4),
      Shade_300 = Color(0xFFA1887F),
      Shade_400 = Color(0xFF8D6E63),
      Shade_500 = Color(0xFF795548),
      Shade_600 = Color(0xFF6D4C41),
      Shade_700 = Color(0xFF5D4037),
      Shade_800 = Color(0xFF4E342E),
      Shade_900 = Color(0xFF3E2723),
    )
    val defaultGray = ColorPalettes(
      Shade_50 = Color(0xFFFAFAFA),
      Shade_100 = Color(0xFFF5F5F5),
      Shade_200 = Color(0xFFEEEEEE),
      Shade_300 = Color(0xFFE0E0E0),
      Shade_400 = Color(0xFFBDBDBD),
      Shade_500 = Color(0xFF9E9E9E),
      Shade_600 = Color(0xFF757575),
      Shade_700 = Color(0xFF616161),
      Shade_800 = Color(0xFF424242),
      Shade_900 = Color(0xFF212121),
    )
    val defaultBlueGray = ColorPalettes(
      Shade_50 = Color(0xFFECEFF1),
      Shade_100 = Color(0xFFCFD8DC),
      Shade_200 = Color(0xFFB0BEC5),
      Shade_300 = Color(0xFF90A4AE),
      Shade_400 = Color(0xFF78909C),
      Shade_500 = Color(0xFF607D8B),
      Shade_600 = Color(0xFF546E7A),
      Shade_700 = Color(0xFF455A64),
      Shade_800 = Color(0xFF37474F),
      Shade_900 = Color(0xFF263238),
    )
  }
}

class ColorPalettes(
  val Shade_50: Color,
  val Shade_100: Color,
  val Shade_200: Color,
  val Shade_300: Color,
  val Shade_400: Color,
  val Shade_500: Color,
  val Shade_600: Color,
  val Shade_700: Color,
  val Shade_800: Color,
  val Shade_900: Color,
) {

  val current
    @Composable get() = when {
      isSystemInDarkTheme() -> Shade_800
      else -> Shade_400
    }
}


val LocalColorful = compositionChainOf("Colorful") { Colorful.default }
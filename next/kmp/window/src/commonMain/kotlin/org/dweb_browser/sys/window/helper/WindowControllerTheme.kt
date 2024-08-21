package org.dweb_browser.sys.window.helper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.platform.theme.md_theme_dark_inverseOnSurface
import org.dweb_browser.helper.platform.theme.md_theme_dark_onSurface
import org.dweb_browser.helper.platform.theme.md_theme_dark_surface
import org.dweb_browser.helper.platform.theme.md_theme_light_inverseOnSurface
import org.dweb_browser.helper.platform.theme.md_theme_light_onSurface
import org.dweb_browser.helper.platform.theme.md_theme_light_surface
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.helper.asWindowStateColorOr
import kotlin.math.sqrt

/**窗口主题控制器*/
val LocalWindowControllerTheme = compositionChainOf<WindowControllerTheme>("WindowControllerTheme")

class WindowControllerTheme(
  val topContentColor: Color,
  val topBackgroundColor: Color,
  val themeColor: Color,
  val themeContentColor: Color,
  val onThemeColor: Color,
  val onThemeContentColor: Color,
  val bottomContentColor: Color,
  val bottomBackgroundColor: Color,
  val isDark: Boolean,
) {
  val winFrameBrush by lazy {
    Brush.verticalGradient(listOf(topBackgroundColor, themeColor, bottomBackgroundColor))
  }
  val themeDisableColor by lazy { themeColor.copy(alpha = themeColor.alpha * 0.2f) }
  val themeContentDisableColor by lazy { themeContentColor.copy(alpha = themeContentColor.alpha * 0.2f) }
  val onThemeContentDisableColor by lazy { onThemeContentColor.copy(alpha = onThemeContentColor.alpha * 0.5f) }
  val topContentDisableColor by lazy { topContentColor.copy(alpha = topContentColor.alpha * 0.2f) }
  val bottomContentDisableColor by lazy { bottomContentColor.copy(alpha = bottomContentColor.alpha * 0.2f) }

  //  val themeButtonColors by lazy {
//    ButtonColors(
//      contentColor = themeContentColor,
//      containerColor = themeColor,
//      disabledContentColor = themeContentDisableColor,
//      disabledContainerColor = themeDisableColor,
//    )
//  }
//  val themeContentButtonColors by lazy {
//    ButtonColors(
//      contentColor = onThemeContentColor,
//      containerColor = themeContentColor,
//      disabledContentColor = onThemeContentDisableColor,
//      disabledContainerColor = themeContentDisableColor,
//    )
//  }
  @Composable
  fun ThemeButtonColors() = ButtonDefaults.buttonColors(
    themeContentColor, themeColor, themeContentDisableColor, themeDisableColor
  )

  @Composable
  fun ThemeContentButtonColors() = ButtonDefaults.buttonColors(
    onThemeContentColor, themeContentColor, onThemeContentDisableColor, themeContentDisableColor
  )

  class AlertDialogColors(
    val containerColor: Color,
    val iconContentColor: Color,
    val titleContentColor: Color,
    val textContentColor: Color,
  )

  val alertDialogColors by lazy {
    AlertDialogColors(
      containerColor = themeColor,
      iconContentColor = themeContentColor,
      titleContentColor = themeContentColor,
      textContentColor = themeContentColor,
    )
  }
}

/**
 * 构建颜色
 */
@Composable
fun WindowController.buildTheme(): WindowControllerTheme {
//  val calcThemeContentColor = watchedState(dark, watchKey = WindowPropertyKeys.ThemeColor) {
//    themeColor.asWindowStateColor(
//      md_theme_light_surface, md_theme_dark_surface, dark
//    )
//  }
  val colorScheme by watchedState { colorScheme }
  val isSystemInDark = isSystemInDarkTheme()
  val isDark = remember(colorScheme, isSystemInDark) {
    colorScheme.isDarkOrNull ?: isSystemInDark
  }
  val lightContent = remember(isDark) {
    if (isDark) md_theme_dark_onSurface else md_theme_light_inverseOnSurface
  }
  val darkContent = remember(isDark) {
    if (isDark) md_theme_dark_inverseOnSurface else md_theme_light_onSurface
  }

  fun calcContentColor(backgroundColor: Color) =
    if (backgroundColor.luminance() > 0.5f) darkContent else lightContent

  fun Color.convertToDark() = convert(ColorSpaces.Oklab).let { oklab ->
    if (oklab.red > 0.4f) {
      oklab.copy(red = (oklab.red * oklab.red).let { light -> if (light < 0.4f) light else 0.4f })
        .convert(ColorSpaces.Srgb)
    } else this
  }

  fun Color.convertToLight() = convert(ColorSpaces.Oklab).let { oklab ->
    if (oklab.red <= 0.6f) {
      oklab.copy(red = sqrt(oklab.red).let { light -> if (light >= 0.6f) light else 0.6f })
        .convert(ColorSpaces.Srgb)
    } else this
  }

  val themeColors by watchedState(
    isDark, watchKey = WindowPropertyKeys.ThemeColor
  ) {
    fun getThemeColor() = themeColor.asWindowStateColorOr(
      md_theme_light_surface, md_theme_dark_surface, isDark
    )

    val smartThemeColor = if (isDark) themeDarkColor.asWindowStateColorOr {
      getThemeColor().convertToDark()
    } else getThemeColor()
    val themeContentColor = calcContentColor(smartThemeColor)
    val themeOklabColor = smartThemeColor.convert(ColorSpaces.Oklab)
    val onThemeColor =
      themeOklabColor.copy(red = sqrt(themeOklabColor.red), alpha = 0.5f).convert(ColorSpaces.Srgb)
        .compositeOver(themeContentColor)
    val onThemeContentColor = themeOklabColor.copy(red = themeOklabColor.red * themeOklabColor.red)
      .convert(ColorSpaces.Srgb)
    Pair(Pair(smartThemeColor, themeContentColor), Pair(onThemeColor, onThemeContentColor))
  }
  val (themeColor, themeContentColor) = themeColors.first
  val (onThemeColor, onThemeContentColor) = themeColors.second
  val topBackgroundColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.TopBarBackgroundColor
  ) {
    fun getTopBarBackgroundColor() = topBarBackgroundColor.asWindowStateColorOr(themeColor)
    if (isDark) {
      topBarBackgroundDarkColor.asWindowStateColorOr { getTopBarBackgroundColor().convertToDark() }
    } else getTopBarBackgroundColor()
  }
  val topContentColor by watchedState(isDark, watchKey = WindowPropertyKeys.TopBarContentColor) {
    fun getTopBarContentColor() = topBarContentColor.asWindowStateColorOr {
      calcContentColor(
        topBackgroundColor
      )
    }
    if (isDark) {
      topBarContentDarkColor.asWindowStateColorOr { getTopBarContentColor().convertToLight() }
    } else getTopBarContentColor()
  }
  val bottomBackgroundColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.BottomBarBackgroundColor
  ) {
    fun getBottomBarBackgroundColor() = bottomBarBackgroundColor.asWindowStateColorOr(
      themeColor
    )
    if (isDark) {
      bottomBarBackgroundDarkColor.asWindowStateColorOr { getBottomBarBackgroundColor().convertToDark() }
    } else getBottomBarBackgroundColor()
  }
  val bottomContentColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.BottomBarContentColor
  ) {
    fun getBottomBarContentColor() = bottomBarContentColor.asWindowStateColorOr {
      calcContentColor(
        bottomBackgroundColor
      )
    }
    if (isDark) {
      bottomBarContentDarkColor.asWindowStateColorOr { getBottomBarContentColor().convertToLight() }
    } else getBottomBarContentColor()
  }


  return WindowControllerTheme(
    themeColor = themeColor,
    themeContentColor = themeContentColor,
    onThemeColor = onThemeColor,
    onThemeContentColor = onThemeContentColor,
    topBackgroundColor = topBackgroundColor,
    topContentColor = topContentColor,
    bottomBackgroundColor = bottomBackgroundColor,
    bottomContentColor = bottomContentColor,
    isDark = isDark,
  )
}
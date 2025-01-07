package org.dweb_browser.helper.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun SetSystemBarsColor(bgColor: Color, fgColor: Color) {
  // 通过systemUiController来改systemBar颜色
//  findWindow()?.also { window ->
//    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
//    val isDarkIcon = fgColor.luminance() < 0.5f
//    insetsController.isAppearanceLightStatusBars = !isDarkIcon
//    insetsController.isAppearanceLightNavigationBars = !isDarkIcon
//  }

  val systemUiController = rememberSystemUiController()
  SideEffect {
    systemUiController.setSystemBarsColor(
      color = Color.Transparent, darkIcons = fgColor.luminance() < 0.5f
    )
  }
}

@Composable
fun findWindow(): Window? = LocalView.current.let { view ->
  remember {
    (view.parent as? DialogWindowProvider)?.window ?: view.context.findWindow()
  }
}

private tailrec fun Context.findWindow(): Window? = when (this) {
  is Activity -> window
  is ContextWrapper -> baseContext.findWindow()
  else -> null
}
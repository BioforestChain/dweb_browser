package org.dweb_browser.browser.desk.render

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration

@SuppressLint("AnnotateVersionCheck")
actual fun canSupportModifierBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun rememberAndroidDisplaySize(): Size {
  val configuration = LocalConfiguration.current
  val displaySize = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
    Size(
      configuration.screenWidthDp.toFloat(),
      configuration.screenHeightDp.toFloat(),
    )
  }
  return displaySize
}
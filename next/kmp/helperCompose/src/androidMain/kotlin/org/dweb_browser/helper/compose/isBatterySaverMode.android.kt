package org.dweb_browser.helper.compose

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun isBatterySaverMode(): Boolean {
  val context = LocalContext.current
  val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
  return powerManager.isPowerSaveMode
}
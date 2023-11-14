package org.dweb_browser.helper.platform

import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.android.BaseActivity

class PlatformViewController(val activity: BaseActivity) : IPlatformViewController {
  private val context: Context = activity

  override suspend fun getViewWidthPx() = activity.window.decorView.width

  override suspend fun getViewHeightPx() = activity.window.decorView.height

  override suspend fun getDisplayDensity() = activity.resources.displayMetrics.density

  val androidContext get() = context
  override val lifecycleScope: CoroutineScope = activity.lifecycleScope
}

fun IPlatformViewController.asAndroid(): PlatformViewController {
  require(this is PlatformViewController)
  return this
}
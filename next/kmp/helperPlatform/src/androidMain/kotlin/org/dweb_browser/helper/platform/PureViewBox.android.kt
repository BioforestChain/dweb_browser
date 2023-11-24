package org.dweb_browser.helper.platform

import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.android.BaseActivity

actual suspend fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) { PureViewBox(viewController) }
}

class PureViewBox(val activity: BaseActivity) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<BaseActivity, IPureViewBox>()
  }

  private val context: Context = activity

  override suspend fun getViewWidthPx() = activity.window.decorView.width

  override suspend fun getViewHeightPx() = activity.window.decorView.height

  override suspend fun getDisplayDensity() = activity.resources.displayMetrics.density

  val androidContext get() = context
  override val lifecycleScope: CoroutineScope = activity.lifecycleScope
}

fun IPureViewBox.asAndroid(): PureViewBox {
  require(this is PureViewBox)
  return this
}
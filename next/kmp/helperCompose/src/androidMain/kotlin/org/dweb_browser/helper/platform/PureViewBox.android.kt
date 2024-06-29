package org.dweb_browser.helper.platform

import android.content.Context
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
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

  override suspend fun getViewSizePx() = with(activity.window.decorView) { IntSize(width, height) }

  override suspend fun getDisplaySizePx() =
    with(activity.resources.displayMetrics) { IntSize(widthPixels, heightPixels) }

  override suspend fun getViewControllerMaxBoundsPx() =
    with(activity.window.decorView) { IntRect(0,0,width, height) }


  override suspend fun getDisplayDensity() = activity.resources.displayMetrics.density

  val androidContext get() = context
  override val lifecycleScope: CoroutineScope = activity.lifecycleScope
}

fun IPureViewBox.asAndroid(): PureViewBox {
  require(this is PureViewBox)
  return this
}
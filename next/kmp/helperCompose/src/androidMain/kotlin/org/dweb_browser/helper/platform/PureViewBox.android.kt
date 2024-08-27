package org.dweb_browser.helper.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.getOrPut

actual fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) { PureViewBox(viewController) }
}

class PureViewBox internal constructor(val activity: BaseActivity) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<BaseActivity, IPureViewBox>()
  }

  private val context: Context = activity

  override suspend fun getViewSizePx() = with(activity.window.decorView) { IntSize(width, height) }

  override suspend fun getDisplaySizePx() =
    with(activity.resources.displayMetrics) { IntSize(widthPixels, heightPixels) }

  override suspend fun getViewControllerMaxBoundsPx() =
    with(activity.window.decorView) { IntRect(0, 0, width, height) }


  override suspend fun getDisplayDensity() = activity.resources.displayMetrics.density

  val androidContext get() = context
  override val lifecycleScope: CoroutineScope = activity.lifecycleScope
}

fun IPureViewBox.asAndroid(): PureViewBox {
  require(this is PureViewBox)
  return this
}

@Composable
actual fun rememberDisplaySize(): Size {
  val resources = LocalContext.current.resources
  val density = LocalDensity.current.density
  return remember(resources, density) {
    with(resources.displayMetrics) { Size(widthPixels / density, heightPixels / density) }
  }
}
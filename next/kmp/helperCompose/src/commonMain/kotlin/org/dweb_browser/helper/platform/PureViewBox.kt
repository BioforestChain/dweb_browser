package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.compose.div

expect fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox

/**
 * 视图 前端
 */
interface IPureViewBox {
  companion object {}

  val lifecycleScope: CoroutineScope

  /**
   * 获取当前视图的大小（物理级别）
   */
  suspend fun getViewSizePx(): IntSize;

  /**
   * 获取当前视图的大小（逻辑级别，displayPoint）
   */

  suspend fun getViewSize() = getViewSizePx() / getDisplayDensity()

  /**
   * 获取屏幕大小（物理级别）
   */
  suspend fun getDisplaySizePx(): IntSize;

  /**
   * 获取屏幕大小（逻辑级别，displayPoint）
   */
  suspend fun getDisplaySize() = getDisplaySizePx() / getDisplayDensity()

  /**
   * 获取 PVC 在所在环境下，渲染对应的 activity/uiView/window 的 bounds 的最大值
   * 在 Android 和 IOS中，pvc都是虚拟出来的，没有原生的窗口标准可以适配，所以基本等于 rootActivity/rootUiView 的大小作为画布，所以窗口也就能渲染那么大
   * 在 Desktop 中，是有应用栏或者 macos的状态栏的，新版的windows还有用于AI对话的sidebar，所以这里的大小需要规避掉这些安全区域
   */
  suspend fun getViewControllerMaxBoundsPx(): IntRect;
  suspend fun getViewControllerMaxBounds() = getViewControllerMaxBoundsPx() / getDisplayDensity()

  /**
   * 获取当前显示器的像素密度
   */
  suspend fun getDisplayDensity(): Float;

}

@Composable
fun rememberPureViewBox(viewController: IPureViewController? = null) =
  LocalPureViewBox.current ?: (viewController
    ?: LocalPureViewController.current).let { pvc -> remember(pvc) { IPureViewBox.from(pvc) } }

internal val LocalPureViewBox = compositionChainOf<IPureViewBox?>("PureViewBox") { null }

@Composable
expect fun rememberDisplaySize():Size
@Composable
internal fun commonRememberDisplaySize()= rememberPureViewBox().let { viewBox ->
  produceState(Size.Zero) {
    value = viewBox.getDisplaySize()
  }.value
}

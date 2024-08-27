package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.div
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.mainAsyncExceptionHandler

actual fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) {
    viewController.pureViewBox
  }
}

class PureViewBox(
  val pureViewController: PureViewController,
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<IPureViewController, IPureViewBox>()
  }

  override suspend fun getViewSizePx() =
    with(pureViewController.awaitComposeWindow()) { IntSize(width, height) }

  override suspend fun getDisplaySizePx() =
    getDisplaySizePxSync(pureViewController.awaitComposeWindow())

  fun getDisplaySizePxSync(composeWindow: ComposeWindow) =
    with(composeWindow.toolkit.screenSize) { IntSize(width, height) }

  override suspend fun getViewControllerMaxBoundsPx() =
    pureViewController.awaitComposeWindow().getScreenBounds()

  /**
   * 参考算法：
   * GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds.bounds
   * = SunGraphicsEnvironment.getUsableBounds(screenDevice)
   * = public static Rectangle getUsableBounds(GraphicsDevice var0) {
   *     GraphicsConfiguration var1 = var0.getDefaultConfiguration();
   *     Insets var2 = Toolkit.getDefaultToolkit().getScreenInsets(var1);
   *     Rectangle var3 = var1.getBounds();
   *     var3.x += var2.left;
   *     var3.y += var2.top;
   *     var3.width -= var2.left + var2.right;
   *     var3.height -= var2.top + var2.bottom;
   *     return var3;
   *   }
   */
  private fun getViewControllerMaxBoundsPxSync(composeWindow: ComposeWindow) =
    /// 获取安全区域
    with(composeWindow.toolkit.getScreenInsets(composeWindow.graphicsConfiguration)) {
      /// 获取屏幕大小
      with(composeWindow.graphicsConfiguration.bounds) {
        IntRect(left = left, top = top, right = width - right, bottom = height - bottom)
      }
    }

  @Composable
  fun currentViewControllerMaxBounds(withSafeArea: Boolean): Rect {
    val composeWindow by pureViewController.composeWindowAsState()
    val density = LocalDensity.current.density
    return remember(composeWindow, density, withSafeArea) {
      when {
        withSafeArea -> composeWindow.getScreenBounds()
        else -> getDisplaySizePxSync(composeWindow).toIntRect()
      } / density
    }
  }

  override suspend fun getDisplayDensity() = PureViewController.density

  override val lifecycleScope: CoroutineScope =
    CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
}

fun IPureViewBox.asDesktop(): PureViewBox {
  require(this is PureViewBox)
  return this
}

@Composable
actual fun rememberDisplaySize(): Size {
  val composeWindow by LocalPureViewController.current.asDesktop().composeWindowAsState()
  val density = LocalDensity.current.density
  return remember(density, composeWindow.toolkit) {
    with(composeWindow.toolkit.screenSize) { Size(width / density, height / density) }
  }
}
package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.timesToInt
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.withMainContext
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

actual fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) {
    PureViewBox(viewController.uiViewControllerInMain)
  }
}

@OptIn(ExperimentalForeignApi::class)
class PureViewBox(
  val uiViewController: UIViewController,
  private val uiScreen: UIScreen = UIScreen.mainScreen,
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<IPureViewController, IPureViewBox>()
  }

  private var defaultViewWidth = 0
  private var defaultViewHeight = 0
  private var defaultDisplayDensity = 1f

  override suspend fun getViewSizePx() = getViewSize().timesToInt(getDisplayDensity())

  override suspend fun getViewSize() = withMainContext {
    uiViewController.view.frame.useContents {
      Size(
        size.width.toFloat(),
        size.height.toFloat()
      )
    }
  }

  override suspend fun getDisplaySizePx() = getDisplaySize().timesToInt(getDisplayDensity())
  override suspend fun getDisplaySize() = withMainContext {
    uiScreen.bounds.useContents {
      Size(size.width.toFloat(), size.height.toFloat())
    }
  }

  override suspend fun getViewControllerMaxBoundsPx() =
    getViewControllerMaxBounds().timesToInt(getDisplayDensity())

  override suspend fun getViewControllerMaxBounds() = withMainContext {
    uiScreen.applicationFrame().useContents {
      // 这里不使用 origin 获取x、y。因为对于虚拟环境来说，它都是相对于这个 application 进行绘制的
      Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
    }
  }

  override suspend fun getDisplayDensity() =
    runCatching { withMainContext { uiScreen.scale.toFloat() } }.getOrDefault(
      defaultDisplayDensity
    )

  override val lifecycleScope: CoroutineScope =
    CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
}

fun IPureViewBox.asIos(): PureViewBox {
  require(this is PureViewBox)
  return this
}

@OptIn(ExperimentalForeignApi::class, InternalComposeUiApi::class)
@Composable
actual fun rememberDisplaySize(): Size {
  val uiScreen = UIScreen.mainScreen
  return remember(uiScreen, rememberInterfaceOrientation()) {
    uiScreen.bounds.useContents {
      Size(size.width.toFloat(), size.height.toFloat())
    }
  }
}
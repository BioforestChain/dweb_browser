package org.dweb_browser.helper.platform

import androidx.compose.ui.awt.ComposePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.mainAsyncExceptionHandler

actual suspend fun IPureViewBox.Companion.from(viewController: IPureViewController): IPureViewBox {
  require(viewController is PureViewController)
  return PureViewBox.instances.getOrPut(viewController) {
    viewController.pureViewBox
  }
}

class PureViewBox(
  val panel: ComposePanel,
  val pureViewController: PureViewController
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<IPureViewController, IPureViewBox>()
  }

  override suspend fun getViewWidthPx() = panel.width

  override suspend fun getViewHeightPx() = panel.height

  override suspend fun getDisplayDensity() = pureViewController.density

  override val lifecycleScope: CoroutineScope =
    CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
}

fun IPureViewBox.asDesktop(): PureViewBox {
  require(this is PureViewBox)
  return this
}
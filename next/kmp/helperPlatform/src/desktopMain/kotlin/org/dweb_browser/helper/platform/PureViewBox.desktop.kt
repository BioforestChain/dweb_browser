package org.dweb_browser.helper.platform

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
  val pureViewController: PureViewController
) : IPureViewBox {
  companion object {
    internal val instances = WeakHashMap<IPureViewController, IPureViewBox>()
  }

  override suspend fun getViewWidthPx() = pureViewController.awaitComposeWindow().width

  override suspend fun getViewHeightPx() = pureViewController.awaitComposeWindow().height

  override suspend fun getDisplayDensity() = PureViewController.density

  override val lifecycleScope: CoroutineScope =
    CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
}

fun IPureViewBox.asDesktop(): PureViewBox {
  require(this is PureViewBox)
  return this
}
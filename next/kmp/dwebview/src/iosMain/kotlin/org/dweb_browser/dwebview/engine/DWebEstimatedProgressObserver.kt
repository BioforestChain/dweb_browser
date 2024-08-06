package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.platform.ios.KeyValueObserverProtocol
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSNumber
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject


/**
 * 监听加载进度
 */
@OptIn(ExperimentalForeignApi::class)
class DWebEstimatedProgressObserver(val engine: DWebViewEngine) : NSObject(),
  KeyValueObserverProtocol {
  init {
    engine.addObserver(
      observer = this,
      forKeyPath = "estimatedProgress",
      options = NSKeyValueObservingOptionNew,
      context = null
    )
  }

  override fun observeValueForKeyPath(
    keyPath: String?,
    ofObject: Any?,
    change: Map<Any?, *>?,
    context: COpaquePointer?
  ) {
    if (keyPath == "estimatedProgress") {
      val progress = change?.get("new") as? NSNumber
      if (progress != null) {
        if (engine.loadingProgressStateFlow.subscriptionCount.value != 0) {
          engine.lifecycleScope.launch {
            engine.loadingProgressStateFlow.emit(progress.floatValue)
          }
        }
      }
    }
  }

  fun disconnect() {
    engine.removeObserver(
      observer = this,
      forKeyPath = "estimatedProgress",
      context = null
    )
  }
}
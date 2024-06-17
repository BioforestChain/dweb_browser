package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.platform.ios.KeyValueObserverProtocol
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSString
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject


/**
 * 监听加载进度
 */
@OptIn(ExperimentalForeignApi::class)
class DWebTitleObserver(val engine: DWebViewEngine) : NSObject(),
  KeyValueObserverProtocol {
  init {
    engine.addObserver(
      observer = this,
      forKeyPath = "title",
      options = NSKeyValueObservingOptionNew,
      context = null
    )
  }

  val titleFlow = MutableStateFlow("")

  override fun observeValueForKeyPath(
    keyPath: String?,
    ofObject: Any?,
    change: Map<Any?, *>?,
    context: COpaquePointer?,
  ) {
    if (keyPath == "title") {
      val title = change?.get("new") as? NSString
      titleFlow.value = title.toString()
    }
  }

  fun disconnect() {
    engine.removeObserver(
      observer = this,
      forKeyPath = "title",
      context = null
    )
  }
}
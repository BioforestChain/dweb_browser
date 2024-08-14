package org.dweb_browser.dwebview.engine.DWebDelegate

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.engine.DWebUIDelegate
import org.dweb_browser.dwebview.engine.DWebViewEngine

@OptIn(ExperimentalForeignApi::class)
internal fun DWebUIDelegate.hookCreateWindow() {
  createWebViewHooks.add {
    val url = forNavigationAction.request.URL?.absoluteString
    val createDwebviewEngine = DWebViewEngine(
      engine.frame, // TODO use windowFeatures.x/y/width/height
      engine.remoteMM,
      DWebViewOptions(url ?: ""),
      createWebViewWithConfiguration
    )
    engine.mainScope.launch {
      var target = "_self"
      if (forNavigationAction.targetFrame == null) {
        target = "_blank"
      }

      val dwebView = IDWebView.create(createDwebviewEngine)
      createWindowSignal.emit(dwebView)
    }
    DWebUIDelegate.CreateWebViewHookPolicyAllow(createDwebviewEngine)
  }
}
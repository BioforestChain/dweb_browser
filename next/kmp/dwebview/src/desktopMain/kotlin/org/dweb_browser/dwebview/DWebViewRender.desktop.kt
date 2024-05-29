package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel

@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?
) {
  require(this is DWebView)
//  when (viewEngine.browser.renderingMode) {
//    RenderingMode.HARDWARE_ACCELERATED -> {
//      // view渲染到swingPanel上
//      SwingPanel(modifier = modifier, factory = {
////        BrowserView.newInstance(viewEngine.browser)
//        viewEngine.wrapperView
//      })
//      // 取消menuBar的实现，因为这里会破坏windows端的样式
//      //  MenuEffect()
//    }
//
//    RenderingMode.OFF_SCREEN -> {
//      // TODO ime
//      LocalPureViewController.current.asDesktop().composeWindowParams.frameWindowScope?.BrowserView(
//        viewEngine.browser,
//        modifier
//      )
//    }
//  }
  SwingPanel(modifier = modifier, factory = remember {
    {
      viewEngine.wrapperView
    }
  })
}

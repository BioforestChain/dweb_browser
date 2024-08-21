package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.core.WindowController

@Composable
actual fun WindowTopBar(
  win: WindowController,
  modifier: Modifier,
) {
  win.bindNativeViewController()
}

@Composable
private fun WindowController.bindNativeViewController() {
  val scope = rememberCoroutineScope()
  // 拿到当前窗口状态
  val viewController = LocalPureViewController.current.asDesktop().composeWindowParams
  scope.launch {
    // 关闭或者隐藏
    viewController.windowEvents.windowClosing.collect {
      this@bindNativeViewController.tryCloseOrHide()
      // 取消此收集器
      coroutineContext.cancel()
    }
  }

//  // 显示控制菜单
//  scope.launch { this@bindNativeViewController.showMenuPanel() }
//
//  // 控制隐藏
//  scope.launch { this@bindNativeViewController.toggleVisible() }
//  // 最大化
//  scope.launch { this@bindNativeViewController.maximize() }
}
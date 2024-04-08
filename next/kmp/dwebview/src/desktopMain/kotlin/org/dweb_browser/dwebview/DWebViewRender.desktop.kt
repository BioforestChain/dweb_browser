package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop
import java.awt.Menu
import java.awt.MenuBar
import java.awt.MenuItem

@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?
) {
  require(this is DWebView)
  // view渲染到swingPanel上
  SwingPanel(modifier = modifier, factory = {
    viewEngine.wrapperView
  })
  // icon加载
//  iconEffect()

  // 取消menuBar的实现，因为这里会破坏windows端的样式
  //  MenuEffect()
}

@Composable
fun IDWebView.iconEffect() {
  // 拿到当前窗口状态
  val window = LocalPureViewController.current.asDesktop().composeWindowParams
  val scope = rememberCoroutineScope()
  val icon = remember { mutableStateOf("") }
  scope.launch {
    icon.value = getIcon()
  }
  if (icon.value.isNotEmpty()) {
    window.icon = painterResource(icon.value)
  }
}

// 窗口devtools menu菜单
val devtoolsMenuWM = WeakHashMap<ComposeWindow, Menu>()

@Composable
fun IDWebView.MenuEffect() {
  // 拿到当前窗口状态
  val window by LocalPureViewController.current.asDesktop().composeWindowAsState()
  val url by urlStateFlow.collectAsState()
  val devtoolsMenu = remember {
    devtoolsMenuWM.getOrPut(window) {
      Menu("devtools")
    }
  }
  // 有可能不存在menubar，需要自己创建一个
  val menuBar = remember { window.menuBar ?: MenuBar().also { window.menuBar = it } }
  // 注册devtools menu
  DisposableEffect(menuBar, devtoolsMenu) {
    menuBar.add(devtoolsMenu)
    onDispose {
      if (devtoolsMenu.itemCount == 0) {
        menuBar.remove(devtoolsMenu)
      }
    }
  }

  // 添加子菜单，并监听点击事件
  val menuItem = remember {
    MenuItem(url).apply {
      addActionListener {
        asDesktop().viewEngine.browser.devTools().show()
      }
    }
  }
  LaunchedEffect(url) {
    menuItem.label = url
  }
  DisposableEffect(menuItem, devtoolsMenu) {
    devtoolsMenu.add(menuItem)
    onDispose {
      devtoolsMenu.remove(menuItem)
    }
  }
}
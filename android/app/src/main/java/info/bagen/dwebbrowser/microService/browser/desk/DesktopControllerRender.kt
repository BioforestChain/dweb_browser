package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.web.WebView
import org.dweb_browser.browser.web.ui.view.CommonWebView
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.Render


@Composable
fun DesktopController.Render(taskbarController: TaskbarController, microModule: MicroModule) {
  // 通过systemUiController来改systemBar颜色
  val systemUiController = rememberSystemUiController()
  val isDarkTheme = isSystemInDarkTheme()
  SideEffect {
    systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = !isDarkTheme)
  }

  CompositionLocalProvider(
    LocalDesktopView provides createMainDwebView(
      "desktop", getDesktopUrl().toString()
    ),
    LocalWindowMM provides microModule,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      /// 桌面视图
      val desktopView = LocalDesktopView.current
      WebView(
        state = desktopView.state,
        navigator = desktopView.navigator,
        modifier = Modifier.fillMaxSize(),
      ) {
        desktopView.webView
      }
      /// 窗口视图
      desktopWindowsManager.Render()
      CommonWebView()
    }
    /// 悬浮框
    Box(modifier = Modifier.fillMaxSize(), Alignment.TopStart) {
      taskbarController.taskbarView.FloatWindow()
    }
    /// 错误信息
    for (message in alertMessages) {
      key(message) {
        val dismissHandler = {
          alertMessages.remove(message);
        }
        AlertDialog(onDismissRequest = {
          dismissHandler()
        }, icon = {
          Icon(Icons.TwoTone.Error, contentDescription = "error")
        }, title = {
          Text(message.title)
        }, text = {
          Text(message.message)
        }, confirmButton = {
          Button(onClick = { dismissHandler() }) {
            Text("关闭")
          }
        })
      }
      break
    }
  }
}
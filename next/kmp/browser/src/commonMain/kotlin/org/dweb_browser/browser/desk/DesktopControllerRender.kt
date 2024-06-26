package org.dweb_browser.browser.desk

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.SetSystemBarsColor
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.SceneRender

@Composable
fun DesktopController.Render(
  desktopController: DesktopController,
  taskbarController: TaskbarController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  // TODO 这里的颜色应该是自动适应的，特别是窗口最大化的情况下，遮盖了顶部 status-bar 的时候，需要根据 status-bar 来改变颜色
  SetSystemBarsColor(Color.Transparent, if (isSystemInDarkTheme()) Color.White else Color.Black)
  LocalCompositionChain.current.Provider(
    LocalWindowMM provides microModule,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      if (remember { envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) }) {
        NewDesktopView(desktopController, microModule)
      } else {
        DesktopView {
          Render(Modifier.fillMaxSize())
        }
      }

      /// 窗口视图
      DesktopWindowsManager {
        SceneRender()
      }
    }
    /// 悬浮框
    Box(contentAlignment = Alignment.TopStart, modifier = Modifier) {
      taskbarController.TaskbarView { FloatWindow() }
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
    /// 新版本
    // newVersionController.NewVersionView() // 先不搞这个
  }
}



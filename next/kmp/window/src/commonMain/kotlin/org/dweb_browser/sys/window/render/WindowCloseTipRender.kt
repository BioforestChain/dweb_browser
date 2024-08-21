package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.watchedState

@OptIn(LowLevelWindowAPI::class)
@Composable
fun WindowController.RenderCloseTip() {
  val win = this
  val winTheme = LocalWindowControllerTheme.current
  val scope = rememberCoroutineScope()

  // 是否显示窗口关闭的提示
  val showCloseTip by win.watchedState { showCloseTip }
  /// 显示关闭窗口的提示框
  if (showCloseTip) {
    val alertDialogColors = winTheme.alertDialogColors;
    /// 会不会有人专门监听showCloseTip然后一直动态地控制closeTip参数呀？
    // 不知道呀
    AlertDialog(
      containerColor = alertDialogColors.containerColor,
      iconContentColor = alertDialogColors.iconContentColor,
      titleContentColor = alertDialogColors.titleContentColor,
      textContentColor = alertDialogColors.textContentColor,
      // 按钮以外的关闭对话框的行为
      onDismissRequest = {
        /// 强制关闭窗口
        scope.launch { win.close(true) }
      },
      // 图标
      icon = {
        win.IconRender(
          modifier = Modifier.size(24.0.dp) // IconButtonTokens.IconSize
        )
      },
      // 标题
      title = {
        Text(
          text = if (win.isMainWindow) WindowI18nResource.application_will_be_close()
          else WindowI18nResource.window_will_be_close()
        )
      },
      // 内容
      text = {
        Text(text = WindowI18nResource.window_confirm_to_close())
      },
      // 确定按钮
      confirmButton = {
        TextButton(
          onClick = {
            /// 强制关闭窗口
            scope.launch { win.close(true) }
          }, colors = winTheme.ThemeContentButtonColors()
        ) {
          Text(WindowI18nResource.window_confirm())
        }
      },
      // 取消按钮
      dismissButton = {
        TextButton(onClick = {
          scope.launch { win.hideCloseTip() }
        }, colors = winTheme.ThemeButtonColors()) {
          Text(WindowI18nResource.window_dismiss())
        }
      },
      // 这个对话框可以通过返回按钮来关闭，同时触发窗口关闭
      properties = DialogProperties(dismissOnClickOutside = false)
    )

  }
}
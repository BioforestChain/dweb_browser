package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.launch
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.PureViewController
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JDialog

@Composable
actual fun NativeDialog(
  onCloseRequest: () -> Unit,
  properties: NativeDialogProperties,
  setContent: @Composable () -> Unit,
) {
  val scope = rememberUpdatedState(rememberCoroutineScope())
  val fromWindow =
    (LocalPureViewController.current as PureViewController).composeWindowAsState().value
  val closeRequestState = rememberUpdatedState(onCloseRequest)
  val dialog = remember {
    ComposeDialog(fromWindow, Dialog.ModalityType.MODELESS).apply {
      preferredSize = fromWindow.size
      minimumSize = Dimension(DialogMinWidth.value.toInt(), DialogMinHeight.value.toInt())
      maximumSize = Dimension(DialogMaxWidth.value.toInt(), DialogMaxHeight.value.toInt())
      /// 禁用默认的关闭按钮的行为
      defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
      addWindowListener(object : WindowAdapter() {
        @Override
        override fun windowClosing(e: WindowEvent?) {
          scope.value.launch {
            closeRequestState.value()
          }
        }
      })
    }
  }

  dialog.title = properties.title ?: fromWindow.title

  // 模态框，但是不能用 dialog.isModal = properties.modal ，否则父窗口的事件循环就卡组了
  dialog.isAlwaysOnTop = properties.modal


  dialog.setIconImage(properties.icon?.toAwtImage() ?: fromWindow.iconImage)
  dialog.compositionLocalContext = currentCompositionLocalContext
  remember(setContent, onCloseRequest) {
    dialog.setContent {
      Box(Modifier.sizeIn(minWidth = DialogMinWidth, maxWidth = DialogMaxWidth)
        .onGloballyPositioned {
          println("QAQ window content size=${it.size}")
        }) {
        setContent()
      }
    }
  }
  DisposableEffect(dialog) {
    dialog.isVisible = true
    dialog.requestFocus()
    onDispose {
      dialog.isVisible = false
      dialog.dispose()
      fromWindow.requestFocus()
    }
  }
}
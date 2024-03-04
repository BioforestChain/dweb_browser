package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.platform.desktop.window.EmptyWindowListener
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowPadding
import java.awt.event.WindowEvent
import javax.swing.JOptionPane

@Composable
internal actual fun ModalState.RenderCloseTipImpl(
  onConfirmToClose: () -> Unit
) {
  LaunchedEffect(Unit) {
    val options = arrayOf(
      WindowI18nResource.modal_close_tip_close.text,
      WindowI18nResource.modal_close_tip_keep.text
    )
    JOptionPane.showOptionDialog(
      null,
      showCloseTip.value,
      when (this@RenderCloseTipImpl) {
        is AlertModal -> WindowI18nResource.modal_close_alert_tip.text
        is BottomSheetsModal -> WindowI18nResource.modal_close_bottom_sheet_tip.text
      },
      JOptionPane.WARNING_MESSAGE,
      JOptionPane.DEFAULT_OPTION,
      null,
      options,
      options[1]
    )
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun BottomSheetsModal.RenderImpl(
  emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean
) {
  val optionPane = remember { JOptionPane(title, JOptionPane.INFORMATION_MESSAGE) }

  /**
   * 统一的关闭信号
   */
  val afterDismiss = CompletableDeferred<Unit>()
  val compositionChainState = rememberUpdatedState(LocalCompositionChain.current)

  val dialog = remember(optionPane, afterDismiss) {
    optionPane.createDialog(title).apply {
      isModal = true
      isVisible = true
      this.addWindowListener(object : EmptyWindowListener() {
        override fun windowClosed(event: WindowEvent) {
          afterDismiss.complete(Unit)
        }
      })
    }
  }
  val composePanel = remember(dialog) { ComposePanel().also { dialog.add(it) } }
  DisposableEffect(afterDismiss, compositionChainState, composePanel) {
    composePanel.setContent {
      val compositionChain by compositionChainState
      compositionChain.Provider(LocalCompositionChain.current) {
        val winPadding = LocalWindowPadding.current
        Column {
          /// banner
          TitleBarWithCustomCloseBottom(
            /// 使用原生的UIKitView来做关闭按钮，所以这里只是做一个简单的占位
            { modifier ->
              Box(modifier)
            }) {
          }

          /// 显示内容
          BoxWithConstraints(
            Modifier.padding(
              start = winPadding.left.dp,
              end = winPadding.right.dp,
              bottom = winPadding.bottom.dp
            )
          ) {
            val windowRenderScope = remember(winPadding, maxWidth, maxHeight) {
              WindowRenderScope.fromDp(maxWidth, maxHeight, 1f)
            }
            windowAdapterManager.Renderer(
              renderId,
              windowRenderScope,
              Modifier.clip(winPadding.contentRounded.toRoundedCornerShape())
            )
          }
        }
      }
    }
    onDispose {
      composePanel.setContent { }
    }
  }
}
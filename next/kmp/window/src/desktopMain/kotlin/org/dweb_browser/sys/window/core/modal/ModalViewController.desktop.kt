package org.dweb_browser.sys.window.core.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.DesktopPureDialogState
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.ModalDialog
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.core.LocalWindowController
import org.dweb_browser.sys.window.render.WithMaterialTheme
import javax.swing.JDialog

internal sealed class ModalViewController<T : ModalState>(
  val modal: T,
) {
  companion object {
    private val modalPvcWHM = WeakHashMap<ModalState, ModalViewController<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : ModalState> from(modal: T) = modalPvcWHM.getOrPut(modal) {
      when (modal) {
        is BottomSheetsModalState -> BottomSheetsModalViewController(modal)
        else -> TODO("AlertModalViewController")
      }
    } as ModalViewController<T>

  }

  @Composable
  protected abstract fun T.Renderer(dialog: JDialog)

  @Composable
  fun ShowModal(
    emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean,
    configState: DesktopPureDialogState.() -> Unit = {},
  ) {
    val compositionChain = LocalCompositionChain.current
    val pvc = LocalPureViewController.current.asDesktop()
    pvc.ModalDialog(
      // 窗口被主动关闭
      requestClose = {
        emitModalVisibilityChange(EmitModalVisibilityState.ForceClose)
      }, state = remember(compositionChain, emitModalVisibilityChange) {
        DesktopPureDialogState(
          chain = compositionChain.contact(LocalEmitModalVisibilityChange provides emitModalVisibilityChange),
        )
      }.also(configState)
    ) { dialog ->
      (compositionChain + LocalCompositionChain.current).Provider {
        LocalWindowController.current.WithMaterialTheme {
          if (modal.isShowCloseTip) {
            modal.CommonRenderCloseTip {
              emitModalVisibilityChange(EmitModalVisibilityState.ForceClose)
            }
          }
          modal.Renderer(dialog)
        }
      }
    }
  }
}
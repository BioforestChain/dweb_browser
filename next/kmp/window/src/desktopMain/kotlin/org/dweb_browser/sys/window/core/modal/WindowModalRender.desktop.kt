package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.DesktopPureDialogState
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.ModalDialog
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowLimits
import org.dweb_browser.sys.window.render.LocalWindowPadding
import org.dweb_browser.sys.window.render.MaterialTheme
import javax.swing.JDialog
import javax.swing.JOptionPane
import kotlin.math.max

@Composable
internal actual fun ModalState.RenderCloseTipImpl(
  onConfirmToClose: () -> Unit
) {
  LaunchedEffect(Unit) {
    val options = arrayOf(
      WindowI18nResource.modal_close_tip_close.text, WindowI18nResource.modal_close_tip_keep.text
    )
    JOptionPane.showOptionDialog(
      null, showCloseTip.value, when (this@RenderCloseTipImpl) {
        is AlertModal -> WindowI18nResource.modal_close_alert_tip.text
        is BottomSheetsModal -> WindowI18nResource.modal_close_bottom_sheet_tip.text
      }, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, options[1]
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun BottomSheetsModal.RenderImpl(
  emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean
) {
  val mvc = remember {
    ModalViewController.from(this)
  }

  val windowLimits = LocalWindowLimits.current

  /**
   * 桌面端允许更大的宽度
   */
  val sheetMaxWidth = remember(windowLimits) {
    max(BottomSheetDefaults.SheetMaxWidth.value, windowLimits.maxWidth * 0.382f)
  }
  mvc.ShowModal(emitModalVisibilityChange) {
    width = sheetMaxWidth
    height = windowLimits.maxHeight
    alignment = Alignment.BottomCenter
  }
}

private class BottomSheetsModalViewController(modal: BottomSheetsModal) :
  ModalViewController<BottomSheetsModal>(modal) {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun BottomSheetsModal.Renderer(dialog: JDialog) {
    val emitModalVisibilityChange = LocalEmitModalVisibilityChange.current

    /**
     * 桌面版必须完全展开，从而符合桌面用户的使用自觉
     */
    val sheetState =
      rememberModalBottomSheetState(true, confirmValueChange = remember(emitModalVisibilityChange) {
        {
          debugModal("confirmValueChange", " $it")
          when (it) {
            SheetValue.Hidden -> modal.isClose
            SheetValue.Expanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
            SheetValue.PartiallyExpanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
          }
        }
      });
    val scope = rememberCoroutineScope()

    val modalWindowInsets = remember {
      WindowInsets(0, 0, 0, 0)
    }

    val winPadding = LocalWindowPadding.current

    /// win/mac 的标准桌面目前不需要提供inset，即便是有刘海屏幕的mac
    val windowInsetTop = 0.dp
    val windowInsetBottom = 0.dp

    val windowLimits = LocalWindowLimits.current

    ModalBottomSheet(sheetState = sheetState,
      modifier = Modifier.padding(top = windowInsetTop),
      // 桌面端允许铺满dialog宽度，因为有原生的dialog宽度来拖兜底
      sheetMaxWidth = windowLimits.maxWidth.dp,
      dragHandle = {
        TitleBarWithOnClose({
          if (emitModalVisibilityChange(EmitModalVisibilityState.TryClose)) {
            scope.launch {
              sheetState.hide()
            }
          }
        }) {
          BottomSheetDefaults.DragHandle(Modifier.align(Alignment.TopCenter))
        }
      },
      windowInsets = modalWindowInsets,
      onDismissRequest = { emitModalVisibilityChange(EmitModalVisibilityState.TryClose) }) {
      /// 显示内容
      BoxWithConstraints(
        Modifier.padding(
          start = winPadding.start.dp,
          end = winPadding.end.dp,
          bottom = windowInsetBottom + windowInsetTop
        )
      ) {
        val windowRenderScope = remember(winPadding, maxWidth, maxHeight) {
          WindowContentRenderScope.fromDp(maxWidth, maxHeight, 1f)
        }
        windowAdapterManager.Renderer(
          renderId, windowRenderScope, Modifier.clip(winPadding.contentRounded.roundedCornerShape)
        )
      }
    }
  }

}

private sealed class ModalViewController<T : ModalState>(
  val modal: T,
) {
  companion object {
    private val modalPvcWHM = WeakHashMap<ModalState, ModalViewController<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : ModalState> from(modal: T) = modalPvcWHM.getOrPut(modal) {
      when (modal) {
        is BottomSheetsModal -> BottomSheetsModalViewController(modal)
        else -> TODO("AlertModalViewController")
      }
    } as ModalViewController<T>

  }

  @Composable
  protected abstract fun T.Renderer(dialog: JDialog)

  @Composable
  fun ShowModal(
    emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean,
    configState: DesktopPureDialogState.() -> Unit = {}
  ) {
    val compositionChain = LocalCompositionChain.current
    val pvc = LocalPureViewController.current.asDesktop()
    pvc.ModalDialog(
      // 窗口被主动关闭
      requestClose = {
        emitModalVisibilityChange(EmitModalVisibilityState.ForceClose)
      }, state = remember(compositionChain, emitModalVisibilityChange) {
        DesktopPureDialogState(
          chain = compositionChain.merge(LocalEmitModalVisibilityChange provides emitModalVisibilityChange),
        )
      }.also(configState)
    ) { dialog ->
      compositionChain.Provider(LocalCompositionChain.current) {
        LocalWindowController.current.MaterialTheme {
          modal.Renderer(dialog)
        }
      }
    }
  }
}

private val LocalEmitModalVisibilityChange =
  compositionChainOf<(state: EmitModalVisibilityState) -> Boolean>("emitModalVisibilityChange")
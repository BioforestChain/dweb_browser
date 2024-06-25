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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowLimits
import org.dweb_browser.sys.window.render.LocalWindowPadding
import javax.swing.JDialog

internal class BottomSheetsModalViewController(modal: BottomSheetsModalState) :
  ModalViewController<BottomSheetsModalState>(modal) {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun BottomSheetsModalState.Renderer(dialog: JDialog) {
    val emitModalVisibilityChange = LocalEmitModalVisibilityChange.current

    /**
     * 桌面版必须完全展开，从而符合桌面用户的使用自觉
     */
    val sheetState =
      rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = remember(emitModalVisibilityChange) {
          {
            debugModal("confirmValueChange", " $it")
            when (it) {
              SheetValue.Hidden -> modal.isClose
              SheetValue.Expanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
              SheetValue.PartiallyExpanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
            }
          }
        })

    val winPadding = LocalWindowPadding.current

    /// win/mac 的标准桌面目前不需要提供inset，即便是有刘海屏幕的mac
    val windowInsetTop = winPadding.top.dp
    val windowInsetBottom = winPadding.bottom.dp
    val density = LocalDensity.current.density

    val windowLimits = LocalWindowLimits.current

    ModalBottomSheet(sheetState = sheetState,
      // 桌面端允许铺满dialog宽度，因为有原生的dialog宽度来拖兜底
      sheetMaxWidth = windowLimits.maxWidth.dp,
      dragHandle = {
        TitleBarWithOnClose({
          emitModalVisibilityChange(EmitModalVisibilityState.TryClose)
        }) {
          BottomSheetDefaults.DragHandle(Modifier.align(Alignment.TopCenter))
        }
      },
      /// dialog 的布局算法会自己算上安全区域，所以这里不需要做任何的 insets 的注入
      windowInsets = WindowInsets(0),
      scrimColor = Color.Transparent,
      onDismissRequest = { emitModalVisibilityChange(EmitModalVisibilityState.TryClose) }) {
      /// 显示内容
      BoxWithConstraints(
        Modifier.padding(
          start = winPadding.start.dp,
          end = winPadding.end.dp,
//          bottom = 0.do
        )
      ) {
        val windowRenderScope = remember(winPadding, maxWidth, maxHeight) {
          WindowContentRenderScope(maxWidth, maxHeight)
        }
        windowAdapterManager.Renderer(
          renderId, windowRenderScope, Modifier.clip(winPadding.contentRounded.roundedCornerShape)
        )
      }
    }
  }

}
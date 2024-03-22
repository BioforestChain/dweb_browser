package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowPadding

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = {
      showCloseTip.value = ""
    },
    title = {
      Text(
        text = when (this) {
          is AlertModal -> WindowI18nResource.modal_close_alert_tip()
          is BottomSheetsModal -> WindowI18nResource.modal_close_bottom_sheet_tip()
        }
      )
    },
    text = { Text(text = showCloseTip.value) },
    confirmButton = {
      ElevatedButton(onClick = { showCloseTip.value = "" }) {
        Text(WindowI18nResource.modal_close_tip_keep())
      }
    },
    dismissButton = {
      Button(onClick = {
        onConfirmToClose()
        // showCloseTip.value = "";
      }) {
        Text(WindowI18nResource.modal_close_tip_close())
      }
    },
  )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun BottomSheetsModal.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
  val sheetState = rememberModalBottomSheetState(confirmValueChange = {
    debugModal("confirmValueChange", " $it")
    when (it) {
      SheetValue.Hidden -> isClose
      SheetValue.Expanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
      SheetValue.PartiallyExpanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
    }
  });
  val scope = rememberCoroutineScope()

  val density = LocalDensity.current
  val defaultWindowInsets = BottomSheetDefaults.windowInsets
  val modalWindowInsets = remember {
    WindowInsets(0, 0, 0, 0)
  }

  val winPadding = LocalWindowPadding.current

  // TODO 这个在Android/IOS上有BUG，会变成两倍大小，需要官方修复
  // https://issuetracker.google.com/issues/307160202
  val windowInsetTop = remember(defaultWindowInsets) {
    (defaultWindowInsets.getTop(density) / density.density / 2).dp
  }
  val windowInsetBottom = remember(defaultWindowInsets) {
    (defaultWindowInsets.getBottom(density) / density.density).dp
  }

  ModalBottomSheet(
    sheetState = sheetState,
    modifier = Modifier.padding(top = windowInsetTop),
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
    onDismissRequest = { emitModalVisibilityChange(EmitModalVisibilityState.TryClose) }
  ) {
    /// 显示内容
    BoxWithConstraints(
      Modifier.padding(
        start = winPadding.start.dp,
        end = winPadding.end.dp,
        bottom = windowInsetBottom + windowInsetTop
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
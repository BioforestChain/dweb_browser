package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.IconRender
import org.dweb_browser.sys.window.render.IdRender
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme
import org.dweb_browser.sys.window.render.LocalWindowPadding

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = {
      showCloseTip.value = ""
    },
    title = { Text(text = "是否关闭抽屉面板") },
    text = { Text(text = showCloseTip.value) },
    confirmButton = {
      ElevatedButton(onClick = { showCloseTip.value = "" }) {
        Text("留下")
      }
    },
    dismissButton = {
      Button(onClick = {
        onConfirmToClose()
        // showCloseTip.value = "";
      }) {
        Text("关闭")
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
      SheetValue.Hidden -> emitModalVisibilityChange(EmitModalVisibilityState.TryClose)
      SheetValue.Expanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
      SheetValue.PartiallyExpanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
    }
  });

  val density = LocalDensity.current
  val defaultWindowInsets = BottomSheetDefaults.windowInsets
  val modalWindowInsets = remember {
    WindowInsets(0, 0, 0, 0)
  }

  val win = parent;
  val winPadding = LocalWindowPadding.current
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.topContentColor

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
      Box(
        modifier = Modifier
          .height(48.dp)
          .fillMaxSize()
          .padding(horizontal = 14.dp),
        Alignment.Center
      ) {
        BottomSheetDefaults.DragHandle()
        /// 应用图标
        Box(
          modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart
        ) {
          win.IconRender(
            modifier = Modifier.size(28.dp), primaryColor = contentColor
          )
        }
        /// 应用身份
        win.IdRender(
          Modifier
            .align(Alignment.BottomEnd)
            .height(22.dp)
            .padding(vertical = 2.dp)
            .alpha(0.4f),
          contentColor = contentColor
        )
      }
    },
    windowInsets = modalWindowInsets,
    onDismissRequest = { emitModalVisibilityChange(EmitModalVisibilityState.TryClose) }) {
    /// 显示内容
    BoxWithConstraints(
      Modifier.padding(
        start = winPadding.left.dp,
        end = winPadding.right.dp,
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
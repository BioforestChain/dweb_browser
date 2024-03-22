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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowPadding
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal actual fun BottomSheetsModal.RenderImpl(
  emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean
) {
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


// @Composable {
//    val optionPane = remember { JOptionPane(title, JOptionPane.INFORMATION_MESSAGE) }
//
//    /**
//     * 统一的关闭信号
//     */
//    val afterDismiss = CompletableDeferred<Unit>()
//    val compositionChainState = rememberUpdatedState(LocalCompositionChain.current)
//
//    val dialog = remember(optionPane, afterDismiss) {
//      optionPane.createDialog(title).apply {
//        isModal = true
//        isVisible = true
//        this.addWindowListener(object : EmptyWindowListener() {
//          override fun windowClosed(event: WindowEvent) {
//            afterDismiss.complete(Unit)
//          }
//        })
//      }
//    }
//    val composePanel = remember(dialog) { ComposePanel().also { dialog.add(it) } }
//    DisposableEffect(afterDismiss, compositionChainState, composePanel) {
//      composePanel.setContent {
//        val compositionChain by compositionChainState
//        compositionChain.Provider(LocalCompositionChain.current) {
//          val winPadding = LocalWindowPadding.current
//          Column {
//            /// banner
//            TitleBarWithCustomCloseBottom(
//              /// 使用原生的UIKitView来做关闭按钮，所以这里只是做一个简单的占位
//              { modifier ->
//                Box(modifier)
//              }) {
//            }
//
//            /// 显示内容
//            BoxWithConstraints(
//              Modifier.padding(
//                start = winPadding.left.dp,
//                end = winPadding.right.dp,
//                bottom = winPadding.bottom.dp
//              )
//            ) {
//              val windowRenderScope = remember(winPadding, maxWidth, maxHeight) {
//                WindowRenderScope.fromDp(maxWidth, maxHeight, 1f)
//              }
//              windowAdapterManager.Renderer(
//                renderId,
//                windowRenderScope,
//                Modifier.clip(winPadding.contentRounded.toRoundedCornerShape())
//              )
//            }
//          }
//        }
//      }
//      onDispose {
//        composePanel.setContent { }
//      }
//    }
//  }
}
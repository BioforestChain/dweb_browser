package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.addMmid
import org.dweb_browser.sys.window.WindowI18nResource
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowPadding
import org.dweb_browser.sys.window.render.WindowPadding
import org.dweb_browser.sys.window.render.idForRender
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UINavigationController
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.modalInPresentation
import platform.UIKit.sheetPresentationController

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  val uiViewController = LocalUIViewController.current
  val alertController = remember {
    UIAlertController.alertControllerWithTitle(
      when (this) {
        is AlertModal -> WindowI18nResource.modal_close_alert_tip.text
        is BottomSheetsModal -> WindowI18nResource.modal_close_bottom_sheet_tip.text
      },
      showCloseTip.value,
      UIAlertControllerStyleAlert
    ).also {
      // 保留Modal
      it.addAction(
        UIAlertAction.actionWithTitle(
          WindowI18nResource.modal_close_tip_keep.text,
          UIAlertActionStyleDefault
        ) {
          showCloseTip.value = ""
        })
      // 关闭Modal
      it.addAction(
        UIAlertAction.actionWithTitle(
          WindowI18nResource.modal_close_tip_close.text,
          UIAlertActionStyleCancel
        ) {
          onConfirmToClose()
        })
    }
  }
  DisposableEffect(Unit) {
    uiViewController.presentViewController(alertController, true, null);
    onDispose {
      alertController.dismissViewControllerAnimated(true) {
        showCloseTip.value = ""
      }
    }
  }
}


@Composable
internal actual fun BottomSheetsModal.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
  val uiViewController = LocalUIViewController.current
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val winPadding = rememberUpdatedState(LocalWindowPadding.current)
  println("QAQ BottomSheetsModal.RenderImpl start")
  @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val pureViewController =
    remember {
      PureViewController(
        params = mapOf(
          "compositionChain" to compositionChain,
          "winPadding" to winPadding
        )
      ).also { pvc ->
        pvc.onCreate { params ->
          pvc.addContent {
            val compositionChain by params["compositionChain"] as State<CompositionChain>
            val winPadding by params["winPadding"] as State<WindowPadding>
            compositionChain.Provider(LocalCompositionChain.current) {
              /// 显示内容
              BoxWithConstraints(
                Modifier.padding(
                  start = winPadding.left.dp,
                  end = winPadding.right.dp,
                  bottom = winPadding.bottom.dp
                )
              ) {
                val windowRenderScope = remember(winPadding) {
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
      }
    }
  /// 一个关闭指令
  val afterDispose = remember { CompletableDeferred<Unit>() }
  LaunchedEffect(Unit) {
    val vc = pureViewController.getUiViewController()
    val nav = UINavigationController(rootViewController = vc)
    nav.modalInPresentation = false
    nav.sheetPresentationController?.also { sheet ->
      // 可以半屏、全屏
      sheet.setDetents(
        listOf(
          UISheetPresentationControllerDetent.mediumDetent(),
          UISheetPresentationControllerDetent.largeDetent(),
        )
      )
      // 显示可拖动的手柄
      sheet.setPrefersGrabberVisible(true)
      // 添加窗口标识
      sheet.sourceView?.addMmid(parent.idForRender)
    }
    val afterPresent = CompletableDeferred<Unit>()
    uiViewController.presentViewController(
      viewControllerToPresent = nav,
      animated = true,
      completion = {
        emitModalVisibilityChange(EmitModalVisibilityState.Open)
        afterPresent.complete(Unit)
      });
    pureViewController.onDestroy {
      println("QAQ BottomSheetsModal.RenderImpl onDestroy")
      emitModalVisibilityChange(EmitModalVisibilityState.ForceClose)
    }
    // 等待显示出来
    afterPresent.await()
    // 至少显示200毫秒
    delay(200)
    // 等待Compose级别的关闭指令
    afterDispose.await()
    // 关闭
    nav.dismissViewControllerAnimated(flag = true, null)
  }
  // 返回按钮按下的时候
  parent.GoBackHandler {
    if (emitModalVisibilityChange(EmitModalVisibilityState.TryClose)) {
      afterDispose.complete(Unit)
    }
  }
  /// compose销毁的时候
  DisposableEffect(afterDispose) {
    onDispose {
      afterDispose.complete(Unit)
    }
  }
}
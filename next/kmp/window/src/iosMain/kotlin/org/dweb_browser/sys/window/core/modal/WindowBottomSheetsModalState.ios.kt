package org.dweb_browser.sys.window.core.modal


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.awaitResult
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.toUIColor
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.addMmid
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.render.incForRender
import platform.UIKit.UIAction
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIImage
import platform.UIKit.UINavigationController
import platform.UIKit.UIPresentationController
import platform.UIKit.UISheetPresentationController
import platform.UIKit.UISheetPresentationControllerDelegateProtocol
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UISheetPresentationControllerDetentIdentifierLarge
import platform.UIKit.UISheetPresentationControllerDetentIdentifierMedium
import platform.UIKit.modalInPresentation
import platform.UIKit.navigationItem
import platform.UIKit.sheetPresentationController
import platform.darwin.NSObject

/**
 * 在 compose-multiplatform 1.7-dev 版本以后，uisheet的滚动手势和compose的滚动冲突，前者会导致后者的滚动中断。
 * 但目前没看到代码有相关的提交，所以怀疑是上游 androidx 的代码出了问题。
 * 因此直到官方修复这个问题之前，我们先使用 CommonRenderImpl 来替代渲染
 */
@Composable
internal fun BottomSheetsModalState.RenderImplNative(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
  val uiViewController = LocalUIViewController.current
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val win = parent
  val uiScope = rememberCoroutineScope()
  val sheetUiDelegate by remember {
    lazy {
      object : NSObject(), UISheetPresentationControllerDelegateProtocol {
        val onResize = SimpleSignal()

        /**
         * 统一的关闭信号
         */
        val afterDismiss = CompletableDeferred<Unit>()
        override fun sheetPresentationControllerDidChangeSelectedDetentIdentifier(
          sheetPresentationController: UISheetPresentationController,
        ) {
          uiScope.launch {
            onResize.emit()
          }
          when (sheetPresentationController.selectedDetentIdentifier) {
            UISheetPresentationControllerDetentIdentifierLarge -> {}
            UISheetPresentationControllerDetentIdentifierMedium -> {}
            else -> {}
          }
        }

        override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
          afterDismiss.complete(Unit)
        }
      }
    }
  }
  @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val pureViewController = remember {
    PureViewController(
      params = mapOf(
        "compositionChain" to compositionChain,
        "afterDismiss" to sheetUiDelegate.afterDismiss,
      )
    ).also { pvc ->
      pvc.onCreate { params ->
        pvc.addContent {
          val compositionChain by params["compositionChain"] as State<CompositionChain>
          (compositionChain + LocalCompositionChain.current).Provider {
            val winFrameStyle = LocalWindowFrameStyle.current
            Column {
              /// banner
              TitleBarWithCustomCloseButton(
                /// 使用原生的UIKitView来做关闭按钮，所以这里只是做一个简单的占位
                { modifier ->
                  Box(modifier)
                }) {}

              /// 显示内容
              BoxWithConstraints(
                Modifier.padding(
                  start = winFrameStyle.startWidth.dp,
                  end = winFrameStyle.endWidth.dp,
                  bottom = winFrameStyle.frameSize.bottom.dp
                )
              ) {
                val windowRenderScope = remember(winFrameStyle, maxWidth, maxHeight) {
                  WindowContentRenderScope(maxWidth, maxHeight)
                }
                windowAdapterManager.Renderer(
                  renderId,
                  windowRenderScope,
                  Modifier.clip(winFrameStyle.contentRounded.roundedCornerShape)
                )
              }
            }
          }
        }
      }
    }
  }
  val winFrameStyle = LocalWindowFrameStyle.current
  val winTheme = LocalWindowControllerTheme.current
  /// 关闭按钮
  // 关闭按钮的事件
  val gestureRecognizer = remember(emitModalVisibilityChange) {
    UIAction.actionWithHandler {
      if (emitModalVisibilityChange(EmitModalVisibilityState.TryClose)) {
        sheetUiDelegate.afterDismiss.complete(Unit)
      }
    }
  }
  // 关闭按钮的图片
  val closeButtonImage = remember {
    UIImage.systemImageNamed(name = "xmark.circle.fill")
  }
  // 菜单关闭按钮的样式
  val closeButton = remember(gestureRecognizer) {
    UIBarButtonItem(
      primaryAction = gestureRecognizer, menu = null
    ).also {
      it.title = "Close Bottom Sheet"
      it.image = closeButtonImage
      it.style = UIBarButtonItemStyle.UIBarButtonItemStyleDone
      it.tintColor = winTheme.topContentColor.toUIColor()
    }
  }
  LaunchedEffect(closeButton, emitModalVisibilityChange) {
    val vc = pureViewController.getUiViewController()
    val nav = UINavigationController(rootViewController = vc)
    // 始终保持展开，只能通过点击按钮关闭
    nav.modalInPresentation = true
    /// 配置 sheet 面板
    nav.sheetPresentationController?.also { sheet ->
      // 可以半屏、全屏
      sheet.setDetents(
        listOf(
          UISheetPresentationControllerDetent.mediumDetent(),
          UISheetPresentationControllerDetent.largeDetent(),
        )
      )
      sheet.preferredCornerRadius = winFrameStyle.contentRounded.topStart.toDouble()
      // 当滚动到边缘时，滚动扩展
      sheet.prefersScrollingExpandsWhenScrolledToEdge = true
      // 显示可拖动的手柄
      sheet.setPrefersGrabberVisible(true)
      // 添加窗口标识
      sheet.sourceView?.addMmid(win.incForRender)
      sheet.delegate = sheetUiDelegate
      launch {
        sheetUiDelegate.afterDismiss.awaitResult()
        emitModalVisibilityChange(EmitModalVisibilityState.ForceClose)
      }
    }
    /// 配置关闭按钮
    vc.navigationItem.rightBarButtonItem = closeButton
    val afterPresent = CompletableDeferred<Unit>()
    uiViewController.presentViewController(viewControllerToPresent = nav,
      animated = true,
      completion = {
        emitModalVisibilityChange(EmitModalVisibilityState.Open)
        afterPresent.complete(Unit)
      });
    // 等待显示出来
    afterPresent.await()
    // 至少显示200毫秒
    delay(200)
    // 等待Compose级别的关闭指令
    launch {
      sheetUiDelegate.afterDismiss.awaitResult()
      // 关闭
      nav.dismissViewControllerAnimated(flag = true, null)
    }
  }
  // 返回按钮按下的时候
  win.navigation.GoBackHandler {
    if (emitModalVisibilityChange(EmitModalVisibilityState.TryClose)) {
      sheetUiDelegate.afterDismiss.complete(Unit)
    }
  }

  /// compose销毁的时候
  DisposableEffect(sheetUiDelegate.afterDismiss) {
    afterDestroy.invokeOnCompletion {
      sheetUiDelegate.afterDismiss.complete(Unit)
    }
    onDispose {
      sheetUiDelegate.afterDismiss.complete(Unit)
    }
  }
}


@Composable
internal actual fun BottomSheetsModalState.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
//  CommonRenderImpl(emitModalVisibilityChange)
  RenderImplNative(emitModalVisibilityChange)
}

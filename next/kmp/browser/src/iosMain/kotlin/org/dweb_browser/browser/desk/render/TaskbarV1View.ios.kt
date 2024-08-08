package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.platform.LocalUIKitBackgroundView
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.window.helper.DraggableDelegate
import org.dweb_browser.sys.window.helper.FloatBarShell
import org.dweb_browser.sys.window.helper.UIDragGesture
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.kCALayerMaxXMaxYCorner
import platform.QuartzCore.kCALayerMaxXMinYCorner
import platform.QuartzCore.kCALayerMinXMaxYCorner
import platform.QuartzCore.kCALayerMinXMinYCorner
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView
import platform.darwin.NSObject

actual suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View = TaskbarV1View(controller, webview)

class TaskbarV1View(
  private val taskbarController: TaskbarV1Controller, override val taskbarDWebView: IDWebView,
) : ITaskbarV1View(taskbarController) {
  private val pvc = PureViewController().also { pvc ->
    pvc.addContent {
      FloatBarShell(state) { modifier ->
        RenderImpl(draggableDelegate, modifier)
      }
    }
  }
  private val lifecycleScope = taskbarController.deskNMM.getRuntimeScope()

  @Composable
  override fun Render() {
    DisposableEffect(Unit) {
      lifecycleScope.launch {
        nativeViewController.addOrUpdate(pvc, zIndex = Int.MAX_VALUE - 1, visible = true)
      }
      onDispose {
        lifecycleScope.launch {
          nativeViewController.addOrUpdate(pvc, visible = false)
        }
      }
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  @Composable
  private fun RenderImpl(draggableDelegate: DraggableDelegate, modifier: Modifier) {  //创建毛玻璃效果层
    val isActivityMode by state.floatActivityStateFlow.collectAsState()
    val density = LocalDensity.current.density

    val wkWebView = taskbarDWebView.asIosWebView()
    val dragGesture = remember(wkWebView) {
      UIDragGesture(
        view = wkWebView,
        draggableDelegate = draggableDelegate,
        density = density,
      )
    }
    dragGesture.draggableDelegate = draggableDelegate
    dragGesture.density = density

    val dragGestureRecognizer = remember {
      UIPanGestureRecognizer(target = dragGesture, action = NSSelectorFromString("dragView:"))
    }
    val foregroundBgColor = remember { UIColor.blackColor.colorWithAlphaComponent(alpha = 0.2) }
    val visualEffectView = remember {
      UIVisualEffectView(effect = UIBlurEffect.effectWithStyle(style = UIBlurEffectStyle.UIBlurEffectStyleLight))
    }
    val bgViewSwitcher = remember {
      { isShowBgView: Boolean ->
        wkWebView.mainScope.launch {
          // 在背景遮罩显示的时候，取消背景色；反之，背景遮罩消失的时候，显示自己的背景色
          if (isShowBgView) {
            wkWebView.backgroundColor = UIColor.clearColor
            visualEffectView.setHidden(false)
          } else {
            wkWebView.backgroundColor = foregroundBgColor
            visualEffectView.setHidden(true)
          }
        }
        Unit
      }
    }
    taskbarDWebView.Render(modifier.onSizeChanged {
      wkWebView.mainScope.launch {
        val width = (it.width / density).toDouble()
        val height = (it.height / density).toDouble()
        visualEffectView.setFrame(
          CGRectMake(0.0, 0.0, width, height)
        )
      }
    }, onCreate = {
      withMainContext {
        /// 增加拖动手势
        pvc.getUiViewController().view.addGestureRecognizer(dragGestureRecognizer)

        /// 前景层的背景色渲染
        wkWebView.layer.cornerRadius = 16.0
        wkWebView.layer.maskedCorners =
          kCALayerMinXMinYCorner + kCALayerMinXMaxYCorner + kCALayerMaxXMinYCorner + kCALayerMaxXMaxYCorner
        wkWebView.layer.masksToBounds = true
        bgViewSwitcher(false)

        visualEffectView.setFrame(wkWebView.frame)

        wkWebView.scrollView.insertSubview(visualEffectView, 0)
        visualEffectView.layer.zPosition = -1.0
      }
    })
    val backgroundView = LocalUIKitBackgroundView.current

    LaunchedEffect(wkWebView) {
      lifecycleScope.launchWithMain {}
    }
    if (backgroundView != null && isActivityMode) {
      BackgroundViewRender(backgroundView, bgViewSwitcher)
    }
  }

  /**
   * 渲染背景遮罩层，并且提供事件绑定
   */
  @OptIn(ExperimentalForeignApi::class)
  @Composable
  private fun BackgroundViewRender(backgroundView: UIView, onToggleBgView: (Boolean) -> Unit) {
    val bgTapGesture = remember {
      UIBackgroundViewTapGesture {
        @Suppress("DeferredResultUnused")
        taskbarController.toggleFloatWindow(openTaskbar = false)
      }
    }
    val onTap = remember {
      UITapGestureRecognizer(target = bgTapGesture, action = NSSelectorFromString("tapBackground:"))
    }
    /// 背景遮罩的显示与隐藏
    DisposableEffect(Unit) {
      val job = lifecycleScope.launchWithMain {
        onToggleBgView(true)
        backgroundView.setHidden(false)
        backgroundView.userInteractionEnabled = true

        backgroundView.addGestureRecognizer(onTap)
        backgroundView.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(alpha = 0.5)
      }
      onDispose {
        job.cancel()
        lifecycleScope.launchWithMain {
          backgroundView.removeGestureRecognizer(onTap)
          backgroundView.backgroundColor = UIColor.clearColor
          backgroundView.userInteractionEnabled = false
          backgroundView.setHidden(true)
          onToggleBgView(false)
        }
      }
    }
  }

}

class UIBackgroundViewTapGesture(val onTap: () -> Unit) : NSObject() {
  @OptIn(BetaInteropApi::class)
  @ObjCAction
  fun tapBackground(gesture: UITapGestureRecognizer) {
    onTap()
  }
}

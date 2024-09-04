package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.rememberDisplaySize
import org.dweb_browser.helper.toPureRect
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.window.floatBar.DraggableDelegate
import org.dweb_browser.sys.window.floatBar.FloatBarShell
import org.dweb_browser.sys.window.floatBar.UIDragGesture
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
import platform.UIKit.UIVisualEffectView
import platform.darwin.NSObject

actual suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View = TaskbarV1View(controller, webview)

class TaskbarV1View(
  private val taskbarController: TaskbarV1Controller, override val taskbarDWebView: IDWebView,
) : ITaskbarV1View(taskbarController) {
  private var showMask by mutableStateOf(false)
  private val pvc = PureViewController(fullscreen = false).also { pvc ->
    pvc.addContent {
      val displaySize = rememberDisplaySize()
      FloatBarShell(state, displaySize = displaySize, effectBounds = { bounds ->
        LaunchedEffect(bounds, displaySize, showMask) {
          val pvcBounds = when {
            showMask -> PureRect(0f, 0f, displaySize.width, displaySize.height)
            else -> bounds
          }
          pvc.setBounds(pvcBounds)
        }

        this.requiredSize(bounds.width.dp, bounds.height.dp).let {
          when {
            showMask -> it.offset(x = bounds.x.dp, y = bounds.y.dp)
            else -> it
          }
        }
      }) { modifier ->
        RenderImpl(draggableDelegate, modifier)
      }
    }
  }
  private val lifecycleScope = taskbarController.deskNMM.getRuntimeScope()

  @Composable
  override fun Render() {
    /// 切换zIndex
    val displaySize = rememberDisplaySize()
    LaunchedEffect(Unit) {
      nativeViewController.addOrUpdate(pvc, zIndex = Int.MAX_VALUE - 1000, visible = true)
      /// 需要给一个初始化的bounds，否则compose默认处于一个0x0的区域，是不会触发渲染的
      pvc.setBounds(displaySize.toRect().toPureRect())
    }
  }

  private val visualEffectView =
    UIVisualEffectView(effect = UIBlurEffect.effectWithStyle(style = UIBlurEffectStyle.UIBlurEffectStyleDark))

  private fun bgViewSwitcher(wkWebView: DWebViewEngine, isShowBgView: Boolean) {
    showMask = isShowBgView
    wkWebView.mainScope.launch {
      // 在背景遮罩显示的时候，取消背景色；反之，背景遮罩消失的时候，显示自己的背景色
      if (isShowBgView) {
        wkWebView.backgroundColor = UIColor.clearColor
        visualEffectView.setHidden(false)
      } else {
        wkWebView.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(alpha = 0.2)
        visualEffectView.setHidden(true)
      }
    }
    Unit
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

    // 不能对NSObject的子类对象的属性直接赋值，否则编译器会报没有setter异常，需要通过函数进行赋值
    dragGesture.setParams(draggableDelegate, density)

    val dragGestureRecognizer = remember {
      UIPanGestureRecognizer(target = dragGesture, action = NSSelectorFromString("dragView:"))
    }
    taskbarDWebView.Render(modifier.clip(RoundedCornerShape(16.dp)).onSizeChanged {
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
        pvc.uiViewControllerInMain.view.addGestureRecognizer(dragGestureRecognizer)

        /// 前景层的背景色渲染
        wkWebView.layer.cornerRadius = 16.0
        wkWebView.layer.maskedCorners =
          kCALayerMinXMinYCorner + kCALayerMinXMaxYCorner + kCALayerMaxXMinYCorner + kCALayerMaxXMaxYCorner
        wkWebView.layer.masksToBounds = true
        bgViewSwitcher(wkWebView, false)

        visualEffectView.setFrame(wkWebView.frame)

        wkWebView.scrollView.insertSubview(visualEffectView, 0)
        visualEffectView.layer.zPosition = -1.0
      }
    })

    if (isActivityMode) {
      BackgroundViewRender {
        bgViewSwitcher(wkWebView, it)
      }
    }
  }

  /**
   * 渲染背景遮罩层，并且提供事件绑定
   */
  @Composable
  private fun BackgroundViewRender(onToggleBgView: (Boolean) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).zIndex(-1f)
      .clickableWithNoEffect {
        @Suppress("DeferredResultUnused")
        taskbarController.toggleFloatWindow(openTaskbar = false)
      })
    DisposableEffect(Unit) {
      onToggleBgView(true)
      onDispose {
        onToggleBgView(false)
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

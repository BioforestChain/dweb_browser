package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.launchInMain
import org.dweb_browser.helper.platform.LocalUIKitBackgroundView
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.kCALayerMaxXMaxYCorner
import platform.QuartzCore.kCALayerMaxXMinYCorner
import platform.QuartzCore.kCALayerMinXMaxYCorner
import platform.QuartzCore.kCALayerMinXMinYCorner
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView
) : ITaskbarView(taskbarController) {
  companion object {
    @OptIn(ExperimentalForeignApi::class)
    suspend fun from(taskbarController: TaskbarController) = withMainContext {
      val webView = IDWebView.create(
        CGRectMake(0.0, 0.0, 100.0, 100.0), taskbarController.deskNMM, DWebViewOptions(
          url = taskbarController.getTaskbarUrl().toString(),
          privateNet = true,
          detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore
        ), WKWebViewConfiguration()
      )
      TaskbarView(taskbarController, webView)
    }
  }

  private val pvc = PureViewController().also { pvc ->
    pvc.addContent {
      NormalFloatWindow()
    }
  }
  private val scope = taskbarController.deskNMM.ioAsyncScope

  @OptIn(ExperimentalForeignApi::class)
  @Composable
  override fun TaskbarViewRender() {  //创建毛玻璃效果层
    val isActivityMode by state.composableHelper.stateOf(getter = { floatActivityState })
    val visualEffectView = remember {
      UIVisualEffectView(effect = UIBlurEffect.effectWithStyle(style = UIBlurEffectStyle.UIBlurEffectStyleLight)) as UIVisualEffectView
    }
    val density = LocalDensity.current.density
    LaunchedEffect(isActivityMode) {
      withMainContext {
        visualEffectView.setHidden(!isActivityMode)
      }
    }
    taskbarDWebView.Render(Modifier.zIndex(2f).onSizeChanged {
      val wkWebView = taskbarDWebView.asIosWebView()
      wkWebView.mainScope.launch {
        val width = (it.width / density).toDouble()
        val height = (it.height / density).toDouble()
        visualEffectView.setFrame(
          CGRectMake(0.0, 0.0, width, height)
        )
      }
    }, onCreate = {
      val wkWebView = asIosWebView()
      withMainContext {
        visualEffectView.setFrame(wkWebView.frame)

        wkWebView.scrollView.insertSubview(visualEffectView, 0)
        visualEffectView.layer.zPosition = -1.0
        visualEffectView.layer.cornerRadius = 20.0
        visualEffectView.layer.maskedCorners =
          kCALayerMinXMinYCorner + kCALayerMinXMaxYCorner + kCALayerMaxXMinYCorner + kCALayerMaxXMaxYCorner
        visualEffectView.layer.masksToBounds = true
      }
    })
    val backgroundView = LocalUIKitBackgroundView.current

    if (backgroundView != null && isActivityMode) {
      BackgroundViewRender(backgroundView)
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  @Composable
  fun BackgroundViewRender(backgroundView: UIView) {
    val onTap = remember {
      println("QAQ UITapGestureRecognizer")
      UITapGestureRecognizer().apply {
        val bgTapGesture = UIBackgroundViewTapGesture {
          println("QAQ isActivityMode = false")
          scope.launch {
            taskbarController.toggleFloatWindow(openTaskbar = false)
          }
        }

        addTarget(target = bgTapGesture, action = NSSelectorFromString("tapBackground:"))
      }
    }
    val sync = remember { Mutex() }
    DisposableEffect(Unit) {
      println("QAQ BackgroundViewRender")
      scope.launchInMain {
        sync.withLock {
          if (backgroundView.userInteractionEnabled) {
            println("QAQ DOUBLE BackgroundViewRender")
          }
          backgroundView.setHidden(false)
          backgroundView.userInteractionEnabled = true

          backgroundView.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(alpha = 0.5)
          backgroundView.addGestureRecognizer(onTap)
        }
      }
      onDispose {
        scope.launchInMain {
          sync.withLock {
            backgroundView.removeGestureRecognizer(onTap)
            backgroundView.backgroundColor = UIColor.clearColor
            backgroundView.userInteractionEnabled = false
            backgroundView.setHidden(true)
          }
        }
      }
    }
  }

  @Composable
  override fun FloatWindow() {
    DisposableEffect(Unit) {
      scope.launch {
        nativeViewController.addOrUpdate(pvc, zIndex = Int.MAX_VALUE - 1)
      }
      onDispose {
        scope.launch {
          nativeViewController.remove(pvc)
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
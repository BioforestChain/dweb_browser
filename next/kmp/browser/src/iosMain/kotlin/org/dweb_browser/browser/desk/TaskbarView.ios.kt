package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asIosWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.kCALayerMaxXMaxYCorner
import platform.QuartzCore.kCALayerMaxXMinYCorner
import platform.QuartzCore.kCALayerMinXMaxYCorner
import platform.QuartzCore.kCALayerMinXMinYCorner
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIVisualEffectView
import platform.WebKit.WKWebViewConfiguration

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
    val isActivityMode by state.composableHelper.stateOf { floatActivityState }
    val visualEffectView = remember {
      UIVisualEffectView(effect = UIBlurEffect.effectWithStyle(style = UIBlurEffectStyle.UIBlurEffectStyleLight)) as UIVisualEffectView
    }
    val density = LocalDensity.current.density
    LaunchedEffect(isActivityMode) {
      withMainContext {
        visualEffectView.setHidden(!isActivityMode)
      }
    }
    taskbarDWebView.Render(Modifier.onSizeChanged {
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

fun UIColor.Companion.fromColorInt(color: Int): UIColor {
  return UIColor(
    red = ((color ushr 16) and 0xFF) / 255.0,
    green = ((color ushr 8) and 0xFF) / 255.0,
    blue = ((color) and 0xFF) / 255.0,
    alpha = 1.0
  )
}
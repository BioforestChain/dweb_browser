package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.WARNING
import org.dweb_browser.platform.ios_browser.browserActiveOn
import org.dweb_browser.platform.ios_browser.DwebWebView
import org.dweb_browser.platform.ios_browser.colorSchemeChangedWithColor
import org.dweb_browser.platform.ios_browser.doSearchWithKey
import org.dweb_browser.platform.ios_browser.gobackIfCanDo
import org.dweb_browser.platform.ios_browser.prepareToKmp
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import kotlin.experimental.ExperimentalNativeApi

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""

@kotlinx.cinterop.ExperimentalForeignApi
var iOSViewHolder: DwebWebView? = null

private var browserObserver = BrowserIosWinObserver(::winVisibleChange, ::winClose)

@OptIn(ExperimentalForeignApi::class)
private fun winClose(): Unit {
  iOSViewHolder = null
}

@OptIn(ExperimentalForeignApi::class)
private fun winVisibleChange(isVisible: Boolean): Unit {
  println("mike winVisibleChange")
  iOSViewHolder?.let {
    it.browserActiveOn(isVisible)
  }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowRenderScope
) {

  val iOSDelegate = remember {
    BrowserIosDelegate().apply {
      this.browserViewModel = viewModel
    }
  }

  val iOSDataSource = remember {
    BrowserIosDataSource().apply {
      this.browserViewModel = viewModel
    }
  }

  val iOSView = remember<DwebWebView> {
    var web = iOSViewHolder
    if (web == null) {
      val frame: CValue<CGRect> = CGRectMake(0.0, 0.0, 0.0, 0.0)
      web = DwebWebView(frame, iOSDelegate, iOSDataSource)
      iOSViewHolder = web
    }
    web!!.prepareToKmp()
    web!!
  }

  browserObserver.browserViewModel = viewModel

  key(viewModel.dwebLinkSearch.value) {
    if (viewModel.dwebLinkSearch.value.isNotEmpty()) {
      iOSView.doSearchWithKey(viewModel.dwebLinkSearch.value.toString())
      viewModel.dwebLinkSearch.value = ""
    }
  }

  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()

  fun backHandler() {
    if (!iOSView.gobackIfCanDo()) {
      scope.launch {
        win.tryCloseOrHide()
      }
    }
  }

  // 窗口返回按钮
  win.GoBackHandler { backHandler() }

  iOSView.colorSchemeChangedWithColor(win.state.colorScheme.ordinal)
  Box {
    UIKitView(
      factory = {
        iOSView
      },
      onRelease = {
        println("DwebWebView UIKitView onRelease")
      },
      modifier = modifier,
    )
//    iOSView.setScale(windowRenderScope.scale)
    iOSView.WindowFrameStyleEffect()
  }
}

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@Composable
actual fun CommonSwipeDismiss(
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  modifier: Modifier,
  onRemove: () -> Unit
) {
  WARNING("Not yet implemented CommonSwipeDismiss")
  Row { dismissContent() }
}
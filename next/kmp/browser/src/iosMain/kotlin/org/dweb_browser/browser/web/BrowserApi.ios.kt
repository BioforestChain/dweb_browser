package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.browser.util.isUrl
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.platform.ios_browser.DwebWebView
import org.dweb_browser.platform.ios_browser.browserActiveOn
import org.dweb_browser.platform.ios_browser.browserClear
import org.dweb_browser.platform.ios_browser.colorSchemeChangedWithColor
import org.dweb_browser.platform.ios_browser.doNewTabUrlWithUrl
import org.dweb_browser.platform.ios_browser.doSearchWithKey
import org.dweb_browser.platform.ios_browser.gobackIfCanDo
import org.dweb_browser.platform.ios_browser.prepareToKmp
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.validateValue
import kotlin.experimental.ExperimentalNativeApi

actual fun ImageBitmap.toImageResource(): ImageResource? = null
actual fun getImageResourceRootPath(): String = ""

/*
* 持有iOSViewHolder, iOSViewDelegate, iOSViewDataSource，是为了确保在win不可见，compose不会将iOS浏览器销毁。
* iOS浏览器只有在win close的时候，才会销毁。
* */
@kotlinx.cinterop.ExperimentalForeignApi
private var iOSViewHolder: DwebWebView? = null
private var iOSViewDelegateHolder: BrowserIosDelegate? = null
private var iOSViewDataSourceHolder: BrowserIosDataSource? = null

private var browserObserver = BrowserIosWinObserver(::winVisibleChange, ::winClose)

@OptIn(ExperimentalForeignApi::class)
private fun winClose(): Unit {
  iOSViewHolder?.let {
    it.browserClear()
  }
  iOSViewHolder = null
  iOSViewDelegateHolder = null
  iOSViewDataSourceHolder = null
}

@OptIn(ExperimentalForeignApi::class)
private fun winVisibleChange(isVisible: Boolean): Unit {
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
    when (val delegate = iOSViewDelegateHolder) {
      null -> BrowserIosDelegate().apply {
        browserViewModel = viewModel
        iOSViewDelegateHolder = this
      }

      else -> delegate
    }
  }

  val iOSDataSource = remember {
    when (val dataSource = iOSViewDataSourceHolder) {
      null -> BrowserIosDataSource().apply {
        browserViewModel = viewModel
        iOSViewDataSourceHolder = this
      }

      else -> dataSource
    }
  }

  val iOSView = remember {
    when (val webView = iOSViewHolder) {
      null -> DwebWebView(
        CGRectMake(0.0, 0.0, 0.0, 0.0),
        iOSDelegate,
        iOSDataSource
      ).also {
        iOSViewHolder = it
      }

      else -> webView
    }.also {
      it.prepareToKmp()
    }
  }

  browserObserver.browserViewModel = viewModel

  key(viewModel.dwebLinkSearch.value) {
    if (viewModel.dwebLinkSearch.value.link.isNotEmpty()) {
      when (viewModel.dwebLinkSearch.value.target) {
        AppBrowserTarget.BLANK.type, AppBrowserTarget.SYSTEM.type -> iOSView.doNewTabUrlWithUrl(
          viewModel.dwebLinkSearch.value.link,
          viewModel.dwebLinkSearch.value.target
        )

        AppBrowserTarget.SELF.type -> {
          iOSView.doSearchWithKey(viewModel.dwebLinkSearch.value.link)
        }
      }

      viewModel.dwebLinkSearch.value = DwebLinkSearchItem.Empty
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

  LaunchedEffect(win.state.colorScheme) {
    iOSView.colorSchemeChangedWithColor(win.state.colorScheme.ordinal)
  }

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
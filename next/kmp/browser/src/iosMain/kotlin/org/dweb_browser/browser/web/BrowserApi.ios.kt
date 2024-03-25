package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.platform.ios_browser.DwebWebView
import org.dweb_browser.platform.ios_browser.browserActiveOn
import org.dweb_browser.platform.ios_browser.browserClear
import org.dweb_browser.platform.ios_browser.colorSchemeChangedWithColor
import org.dweb_browser.platform.ios_browser.doNewTabUrlWithUrl
import org.dweb_browser.platform.ios_browser.doSearchWithKey
import org.dweb_browser.platform.ios_browser.gobackIfCanDo
import org.dweb_browser.platform.ios_browser.prepareToKmp
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.WindowFrameStyleEffect
import platform.CoreGraphics.CGRectMake
import kotlin.experimental.ExperimentalNativeApi

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

@OptIn(ExperimentalForeignApi::class)
actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  iOSViewHolder?.let { iOSView ->
    if (dwebLinkSearchItem.link.isNotEmpty()) {
      when (dwebLinkSearchItem.target) {
        AppBrowserTarget.BLANK, AppBrowserTarget.SYSTEM -> iOSView.doNewTabUrlWithUrl(
          dwebLinkSearchItem.link,
          dwebLinkSearchItem.target.type
        )

        AppBrowserTarget.SELF -> {
          iOSView.doSearchWithKey(dwebLinkSearchItem.link)
        }
      }
    }
  }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel,
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope
) {

  val iOSDelegate = remember {
    when (val delegate = iOSViewDelegateHolder) {
      null -> BrowserIosDelegate(viewModel).apply {
        iOSViewDelegateHolder = this
      }

      else -> delegate
    }
  }

  val iOSDataSource = remember {
    when (val dataSource = iOSViewDataSourceHolder) {
      null -> BrowserIosDataSource(viewModel).apply {
        iOSViewDataSourceHolder = this
      }

      else -> dataSource
    }
  }

  val iOSView = remember {
    when (val webView = iOSViewHolder) {
      null -> DwebWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), iOSDelegate, iOSDataSource).also {
        iOSViewHolder = it
      }

      else -> webView
    }.also {
      it.prepareToKmp()
    }
  }

  browserObserver.browserViewModel = viewModel

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
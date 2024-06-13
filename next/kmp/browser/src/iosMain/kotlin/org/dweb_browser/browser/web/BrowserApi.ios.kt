package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.helper.trueAlso
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
@OptIn(ExperimentalForeignApi::class)
private var iOSBrowserView: DwebWebView? = null

@OptIn(ExperimentalForeignApi::class)
private var iOSViewHolderDeferred = CompletableDeferred<Unit>()

private var browserObserver = BrowserIosWinObserver(::winVisibleChange, ::winClose)

@OptIn(ExperimentalForeignApi::class)
private fun winClose() {
  iOSViewHolder?.browserClear()
  iOSViewHolder = null
  iOSViewDelegateHolder?.destory()
  iOSViewDataSourceHolder?.destory()
  iOSViewDelegateHolder = null
  iOSViewDataSourceHolder = null
}

@OptIn(ExperimentalForeignApi::class)
private fun winVisibleChange(isVisible: Boolean) {
  iOSViewHolderDeferred.isCompleted.trueAlso {
    iOSViewHolder!!.browserActiveOn(isVisible)
  }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem) {
  iOSViewHolderDeferred.await()

  iOSViewHolder!!.let { iOSView ->
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
  windowRenderScope: WindowContentRenderScope,
) {
  val iOSDelegate = remember {
    when (val delegate = iOSViewDelegateHolder) {
      null -> BrowserIosDelegate(viewModel).apply {
        iOSViewDelegateHolder = this
      }

      else -> delegate
    }
  }

  val iOSDataSource = remember(viewModel) {
    when (val dataSource = iOSViewDataSourceHolder) {
      null -> BrowserIosDataSource(viewModel).apply {
        iOSViewDataSourceHolder = this
      }

      else -> dataSource
    }
  }

  DisposableEffect(viewModel) {
    onDispose {
      iOSDelegate.destory()
      iOSDataSource.destory()
    }
  }

  iOSBrowserView = remember {
    when (val webView = iOSViewHolder) {
      null -> DwebWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), iOSDelegate, iOSDataSource).also {
        iOSViewHolder = it
      }

      else -> webView
    }.also {
      it.prepareToKmp()
      iOSViewHolderDeferred.complete(Unit)
    }
  }

  browserObserver.browserViewModel = viewModel

  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()

  fun backHandler() {
    if (!iOSBrowserView!!.gobackIfCanDo()) {
      scope.launch {
        win.tryCloseOrHide()
      }
    }
  }

  // 窗口返回按钮
  win.navigation.GoBackHandler { backHandler() }

  LaunchedEffect(win.state.colorScheme) {
    iOSBrowserView!!.colorSchemeChangedWithColor(win.state.colorScheme.ordinal)
  }

  Box {
    UIKitView(
      factory = {
        iOSBrowserView!!
      },
      onRelease = {
        println("DwebWebView UIKitView onRelease")
        iOSBrowserView = null
      },
      modifier = modifier,
    )
//    iOSView.setScale(windowRenderScope.scale)
    iOSBrowserView!!.WindowFrameStyleEffect()
  }
}

actual suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean {
  return false
}

package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jmm.JmmTabs
import org.dweb_browser.browser.web.data.AppBrowserTarget
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.DwebLinkSearchItem
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.platform.ios_browser.DwebWebView
import org.dweb_browser.platform.ios_browser.browserActiveOn
import org.dweb_browser.platform.ios_browser.browserClear
import org.dweb_browser.platform.ios_browser.colorSchemeChangedWithColor
import org.dweb_browser.platform.ios_browser.doNewTabUrlWithUrl
import org.dweb_browser.platform.ios_browser.doSearchWithKey
import org.dweb_browser.platform.ios_browser.gobackIfCanDo
import org.dweb_browser.platform.ios_browser.loadPullMenuConfigWithIsActived
import org.dweb_browser.platform.ios_browser.prepareToKmp
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowFrameStyle
import org.dweb_browser.sys.window.render.UIKitViewInWindow
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
  browserObserver.browserViewModel = viewModel

  val iOSBrowserView = remember {
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


  val win = LocalWindowController.current
  val scope = rememberCoroutineScope()
  // 窗口返回按钮
  win.navigation.GoBackHandler(iOSBrowserView.gobackIfCanDo()) {
    scope.launch {
      win.tryCloseOrHide()
    }
  }

  val isActived by remember {
    mutableStateOf(envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_PULLDOWNWEBMENU))
  }
  // 使用 LaunchedEffect 在 isActived 初始化时执行一次副作用
  LaunchedEffect(isActived) {
    iOSBrowserView.loadPullMenuConfigWithIsActived(isActived)
  }

  LaunchedEffect(win.state.colorScheme) {
    iOSBrowserView.colorSchemeChangedWithColor(win.state.colorScheme.ordinal)
  }
  Box(modifier = modifier) {
    iOSBrowserView.UIKitViewInWindow(modifier = Modifier.fillMaxSize(), LocalWindowFrameStyle.current)
  }
}

actual suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean {
  return false
}

actual suspend fun dwebviewProxyPrepare() {
  globalDefaultScope.launch {
    DWebView.prepare()
  }
}

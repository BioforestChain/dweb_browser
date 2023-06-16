package info.bagen.dwebbrowser.ui.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.datastore.DefaultAllWebEngine
import info.bagen.dwebbrowser.datastore.WebEngine
import info.bagen.dwebbrowser.microService.browser.BrowserController
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.browser.debugBrowser
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM.Companion.jmmController
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.browser.mwebview.CloseWatcher.CloseWatcher
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewController
import info.bagen.dwebbrowser.microService.browser.webview.DWebView
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.CORS_HEADERS
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.qrcode.QRCodeScanState
import info.bagen.dwebbrowser.util.*
import kotlinx.coroutines.*
import org.http4k.core.Response
import org.http4k.lens.Header
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
data class BrowserUIState(
  val browserViewList: MutableList<BrowserWebView> = mutableStateListOf(), // 多浏览器列表
  val currentBrowserBaseView: MutableState<BrowserWebView>,
  val pagerStateContent: MutableState<PagerState?> = mutableStateOf(null), // 用于表示展示内容
  val pagerStateNavigator: MutableState<PagerState?> = mutableStateOf(null), // 用于表示下面搜索框等内容
  val myInstallApp: MutableMap<Mmid, JsMicroModule> = JmmNMM.getAndUpdateJmmNmmApps(), // 系统安装的应用
  val multiViewShow: MutableTransitionState<Boolean> = MutableTransitionState(false),
  val showBottomBar: MutableTransitionState<Boolean> = MutableTransitionState(true), // 用于网页上滑或者下滑时，底下搜索框和导航栏的显示
  val bottomSheetScaffoldState: BottomSheetScaffoldState = BottomSheetScaffoldState(
    bottomSheetState = SheetState(
      skipPartiallyExpanded = false, initialValue = SheetValue.Hidden, skipHiddenState = false
    ), snackbarHostState = SnackbarHostState()
  ),
  val inputText: MutableState<String> = mutableStateOf(""), // 用于指定输入的内容
  val currentInsets: MutableState<WindowInsetsCompat>, // 获取当前界面区域
  val showSearchEngine: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于在输入内容后，显示本地检索以及提供搜索引擎
  val qrCodeScanState: QRCodeScanState = QRCodeScanState(), // 用于判断桌面的显示隐藏

) {

//  init {
//    initStats()
//  }
//
//
//  @SuppressLint("UnrememberedMutableState")
//  fun initStats() {
//    LocalInputText.provides(mutableStateOf(""))
//    LocalShowSearchView.provides(mutableStateOf(false))
//  }
}

/**
 * 用于指定输入的内容
 */
val LocalInputText = compositionLocalOf<MutableState<String>> {
  mutableStateOf("")
}

/**
 * 用于显示搜索的界面，也就是点击搜索框后界面
 */
val LocalShowSearchView = compositionLocalOf<MutableState<Boolean>> {
  mutableStateOf(false)
}

private fun noLocalProvidedFor(name: String): Nothing {
  error("CompositionLocal $name not present")
}

sealed class BrowserIntent {
  object ShowMainView : BrowserIntent()
  object WebViewGoBack : BrowserIntent()
  object AddNewMainView : BrowserIntent()
  class UpdateCurrentBaseView(val currentPage: Int) : BrowserIntent()
  class UpdateBottomViewState(val show: Boolean) : BrowserIntent()
  class UpdateMultiViewState(val show: Boolean, val index: Int? = null) : BrowserIntent()
  class UpdateSearchEngineState(val show: Boolean) : BrowserIntent()
  class SearchWebView(val url: String) : BrowserIntent()
  class RemoveBaseView(val id: Int) : BrowserIntent()
  class OpenDwebBrowser(val mmid: Mmid) : BrowserIntent()
  class SaveHistoryWebSiteInfo(val title: String?, val url: String?) : BrowserIntent()
  object SaveBookWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  object ShareWebSiteInfo : BrowserIntent() // 直接获取当前的界面来保存
  class UpdateInputText(val text: String) : BrowserIntent()

  class UninstallJmmMetadata(val jmmMetadata: JmmMetadata) : BrowserIntent()
  class ShowSnackbarMessage(val message: String, val actionLabel: String? = null) : BrowserIntent()
}

@OptIn(ExperimentalFoundationApi::class)
class BrowserViewModel(private val browserController: BrowserController) : ViewModel() {
  var uiState: BrowserUIState

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  init {
    val browserWebView = getNewTabBrowserView().also {
      uiState = BrowserUIState(
        currentBrowserBaseView = mutableStateOf(it),
        currentInsets = browserController.currentInsets
      )
    }
    uiState.browserViewList.add(browserWebView)
  }

  fun getNewTabBrowserView(url: String? = null): BrowserWebView {
    val (viewItem,closeWatcher) = appendWebViewAsItem(
      createDwebView(""), url ?: "file:///android_asset/browser/newtab/index.html"
    )
    return BrowserWebView(
      viewItem = viewItem, closeWatcher = closeWatcher
    )
  }

  @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
  fun handleIntent(action: BrowserIntent) {
    viewModelScope.launch(ioAsyncExceptionHandler) {
      when (action) {
        is BrowserIntent.ShowMainView -> {
          uiState.browserViewList.lastOrNull()?.also {
            it.show.value = true
          }
        }

        is BrowserIntent.WebViewGoBack -> {
          uiState.currentBrowserBaseView.value.viewItem.navigator.navigateBack()
        }

        is BrowserIntent.UpdateCurrentBaseView -> {
          if (action.currentPage >= 0 && action.currentPage < uiState.browserViewList.size) {
            uiState.currentBrowserBaseView.value = uiState.browserViewList[action.currentPage]
          }
        }

        is BrowserIntent.UpdateBottomViewState -> {
          uiState.showBottomBar.targetState = action.show
        }

        is BrowserIntent.UpdateMultiViewState -> {
          if (action.show) {
            uiState.currentBrowserBaseView.value.controller.capture()
          }
          uiState.multiViewShow.targetState = action.show
          action.index?.let {
            withContext(mainAsyncExceptionHandler) {
              uiState.pagerStateNavigator.value?.scrollToPage(it)
              uiState.pagerStateContent.value?.scrollToPage(it)
            }
          }
        }

        is BrowserIntent.UpdateSearchEngineState -> {
          uiState.showSearchEngine.targetState = action.show
        }

        is BrowserIntent.AddNewMainView -> {
          withContext(mainAsyncExceptionHandler) {
            val itemView = getNewTabBrowserView()
            uiState.browserViewList.add(itemView)
            uiState.currentBrowserBaseView.value = itemView
            delay(100)
            uiState.multiViewShow.targetState = false
            uiState.pagerStateNavigator.value?.scrollToPage(uiState.browserViewList.size - 1)
            uiState.pagerStateContent.value?.scrollToPage(uiState.browserViewList.size - 1)
          }
        }

        is BrowserIntent.SearchWebView -> {
          uiState.showSearchEngine.targetState = false // 到搜索功能了，搜索引擎必须关闭
          uiState.currentBrowserBaseView.value.viewItem.state.content = WebContent.Url(action.url)/*when (val itemView = uiState.currentBrowserBaseView.value) {
            is BrowserWebView -> {
              itemView.state.content = WebContent.Url(action.url)
            }
            else -> {
              // 新增后，将主页界面置为 false，当搜索框右滑的时候，再重新置为 true
              uiState.browserViewList.lastOrNull()?.let {
                it.show.value = false
              }
              // 创建 webview 并且打开
              withContext(mainAsyncExceptionHandler) {
                val webviewId = "#web${webviewId_acc.getAndAdd(1)}"
                val state = WebViewState(WebContent.Url(action.url))
                val coroutineScope = CoroutineScope(CoroutineName(webviewId))
                val navigator = WebViewNavigator(coroutineScope)
                BrowserWebView(
                  webViewId = webviewId,
                  webView = createDwebView(action.url),
                  state = state,
                  coroutineScope = coroutineScope,
                  navigator = navigator
                ).also { item ->
                  for (index in 0 until uiState.browserViewList.size) {
                    val itemView = uiState.browserViewList[index]
                    if (itemView === uiState.currentBrowserBaseView.value) {
                      uiState.browserViewList.removeAt(index)
                      uiState.browserViewList.add(index, item)
                      uiState.currentBrowserBaseView.value = item
                      break
                    }
                  }
                  // uiState.browserViewList.add(uiState.browserViewList.size - 1, item)
                  // uiState.currentBrowserBaseView.value = item
                }
              }
            }
          }*/
        }

        is BrowserIntent.OpenDwebBrowser -> {
          jmmController?.openApp(action.mmid)
        }

        is BrowserIntent.RemoveBaseView -> {
          uiState.browserViewList.removeAt(action.id)
          if (uiState.browserViewList.size == 0) {
            withContext(mainAsyncExceptionHandler) {
              getNewTabBrowserView().also {
                uiState.browserViewList.add(it)
                uiState.currentBrowserBaseView.value = it
                handleIntent(BrowserIntent.UpdateMultiViewState(false))
              }
            }
          }
        }

        is BrowserIntent.SaveHistoryWebSiteInfo -> {
          action.url?.let {
            if (!isNoTrace.value && !it.startsWith("file:///android_asset/")) { // 无痕模式，不保存历史搜索记录
              WebSiteDatabase.INSTANCE.websiteDao().insert(
                WebSiteInfo(title = action.title ?: it, url = it, type = WebSiteType.History)
              )
            }
          }
        }

        is BrowserIntent.SaveBookWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            val url = it.viewItem.state.lastLoadedUrl ?: ""
            if (url.isEmpty() || url.startsWith("file:///android_asset/")) {
              handleIntent(BrowserIntent.ShowSnackbarMessage("无效书签页"))
              return@let
            }
            WebSiteDatabase.INSTANCE.websiteDao().insert(
              WebSiteInfo(
                title = it.viewItem.state.pageTitle ?: "",
                url = url,
                type = WebSiteType.Book,
                icon = it.viewItem.state.pageIcon?.asImageBitmap()
              )
            )
            handleIntent(BrowserIntent.ShowSnackbarMessage("添加书签成功"))
          }
        }

        is BrowserIntent.ShareWebSiteInfo -> {
          uiState.currentBrowserBaseView.value.let {
            if (it.viewItem.state.lastLoadedUrl?.startsWith("file:///android_asset") == true) {
              handleIntent(BrowserIntent.ShowSnackbarMessage("无效的分享"))
              return@let
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, it.viewItem.state.lastLoadedUrl ?: "") // 分享内容
              // putExtra(Intent.EXTRA_SUBJECT, "分享标题")
              putExtra(Intent.EXTRA_TITLE, it.viewItem.state.pageTitle) // 分享标题
            }
            browserController.activity?.startActivity(Intent.createChooser(shareIntent, "分享到"))
          }
        }

        is BrowserIntent.UpdateInputText -> {
          uiState.inputText.value = action.text
        }

        is BrowserIntent.UninstallJmmMetadata -> {
          browserController.uninstallJMM(action.jmmMetadata)
        }

        is BrowserIntent.ShowSnackbarMessage -> {
          withContext(mainAsyncExceptionHandler) {
            uiState.bottomSheetScaffoldState.snackbarHostState.showSnackbar(
              action.message, action.actionLabel
            )
          }
        }
      }
    }
  }

  suspend fun asyncCreateDwebView(url: String): DWebView = withContext(mainAsyncExceptionHandler) {
    DWebView(
      App.appContext, browserController.browserNMM, browserController.browserNMM, DWebView.Options(
        url = url, onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore
      ), null
    )
  }

  fun createDwebView(url: String): DWebView {
    return DWebView(
      App.appContext, browserController.browserNMM, browserController.browserNMM, DWebView.Options(
        url = url, onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore
      ), null
    )
  }

  @Synchronized
  fun appendWebViewAsItem(dWebView: DWebView, url: String): Pair<MultiWebViewController.ViewItem,CloseWatcher>  {
      val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
      val state = WebViewState(WebContent.Url(url))
      val coroutineScope = CoroutineScope(CoroutineName(webviewId))
      val navigator = WebViewNavigator(coroutineScope)
    val viewItem = MultiWebViewController.ViewItem(
        webviewId = webviewId,
        webView = dWebView,
        state = state,
        coroutineScope = coroutineScope,
        navigator = navigator,
      )
    viewItem.webView.webViewClient = DwebBrowserWebViewClient()
    val closeWatcherController = CloseWatcher(viewItem)

    viewItem.webView.webChromeClient = object : WebChromeClient() {
      override fun onCreateWindow(
        view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
      ): Boolean {
        val transport = resultMsg.obj;
        if (transport is WebView.WebViewTransport) {
          viewItem.coroutineScope.launch {
            debugBrowser("opening")
            val dWebView = asyncCreateDwebView("")
            transport.webView = dWebView;
            resultMsg.sendToTarget();

            // 它是有内部链接的，所以等到它ok了再说
            var url = dWebView.getUrlInMain()
            if (url?.isEmpty() != true) {
              dWebView.waitReady()
              url = dWebView.getUrlInMain()
            }
            debugBrowser("opened ${closeWatcherController.consuming.contains(url)}", url)

            /// 内部特殊行为，有时候，我们需要知道 isUserGesture 这个属性，所以需要借助 onCreateWindow 这个回调来实现
            /// 实现 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
            if (closeWatcherController.consuming.remove(url)) {
              val consumeToken = url!!
              closeWatcherController.apply(isUserGesture).also {
                withContext(mainAsyncExceptionHandler) {
                  dWebView.destroy()
                  closeWatcherController.resolveToken(consumeToken, it)
                }
              }
            } else {
              /// 打开一个新窗口
              runBlockingCatching(Dispatchers.Main) {
                appendWebViewAsItem(
                  dWebView, url ?: "file:///android_asset/browser/newtab/index.html"
                )
              }
            }
          }
          return true
        }
        return super.onCreateWindow(
          view, isDialog, isUserGesture, resultMsg
        )
      }
    }
    return  Pair(viewItem,closeWatcherController)
  }

  val isNoTrace = mutableStateOf(App.appContext.getBoolean(KEY_NO_TRACE, false))
  fun saveBrowserMode(noTrace: Boolean) {
    isNoTrace.value = noTrace
    App.appContext.saveBoolean(KEY_NO_TRACE, noTrace)
  }


  val isShowKeyboard
    get() = uiState!!.currentInsets.value.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0

}


class browserViewModelHelper {
  companion object {
    fun saveLastKeyword(inputText: MutableState<String>, url: String) {
      App.appContext.saveString(KEY_LAST_SEARCH_KEY, url)
      inputText.value = url
    }
  }
}

internal class DwebBrowserWebViewClient : AccompanistWebViewClient() {
  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      request?.url?.let { uri ->
        val url = uri.toString()
        if (url.startsWith("http") || url.startsWith("file") || url.startsWith("ftp")) {
          return super.shouldOverrideUrlLoading(view, request)
        }
        // 暂时不跳转
        /*try {
          App.appContext.startActivity(Intent(Intent.ACTION_VIEW, request.url).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          })
        } catch (_: Exception) {
        }*/
      }
      return true
    }
    return super.shouldOverrideUrlLoading(view, request)
  }

  override fun onReceivedError(
    view: WebView, request: WebResourceRequest?, error: WebResourceError?
  ) {
    // super.onReceivedError(view, request, error)
    if (error?.errorCode == -2 && App.appContext.getString(KEY_LAST_SEARCH_KEY) == request?.url?.toString()) { // net::ERR_NAME_NOT_RESOLVED
      val param = request.url?.let { uri -> "?text=${uri.host}${uri.path}" } ?: ""
      view.loadUrl("file:///android_asset/error.html$param")
    }
  }

  override fun shouldInterceptRequest(
    view: WebView, request: WebResourceRequest
  ): WebResourceResponse? {
    var response: Response? = null
//    if(request.url.scheme == "data")
    if (request.url.host == "localhost") { // 拦截 browser web

      val mmid = request.url.getQueryParameter("mmid")
      var path = request.url.path
      if (mmid !== null) {
        path = path?.replace("browser.dweb", mmid)
      }
      debugBrowser(
        "shouldInterceptRequest localhost=>", "file:/${path}?${request.url.query}"
      )
      response = runBlockingCatching(ioAsyncExceptionHandler) {
        BrowserNMM.browserController?.browserNMM?.nativeFetch("file:/${path}?${request.url.query}")
      }.getOrThrow()
    } else if (request.url.scheme == "dweb") { // 负责拦截browser的dweb_deeplink
      debugBrowser("shouldInterceptRequest dweb=>", request.url)
      response = runBlockingCatching(ioAsyncExceptionHandler) {
        BrowserNMM.browserController?.browserNMM?.nativeFetch(request.url.toString())
      }.getOrThrow()
    }
    if (response !== null) {
      val contentType = Header.CONTENT_TYPE(response)
      return WebResourceResponse(
        contentType?.value,
        contentType?.directives?.find { it.first == "charset" }?.second,
        response.status.code,
        response.status.description,
        CORS_HEADERS.toMap(),
        response.body.stream,
      )
    }
    return super.shouldInterceptRequest(view, request)
  }
}

/**
 * 根据内容解析成需要显示的内容
 */
internal fun parseInputText(text: String, needHost: Boolean = true): String {
  val uri = Uri.parse(text)
  for (item in DefaultAllWebEngine) {
    if (item.fit(text)) return uri.getQueryParameter(item.queryName())!!
  }
  return if (needHost && uri.host?.isNotEmpty() == true) {
    uri.host!!
  } else if (uri.getQueryParameter("text")?.isNotEmpty() == true) {
    uri.getQueryParameter("text")!!
  } else {
    text
  }
}

/**
 * 根据内容来判断获取引擎
 */
internal fun findWebEngine(url: String): WebEngine? {
  for (item in DefaultAllWebEngine) {
    if (item.fit(url)) return item
  }
  return null
}

internal fun String.isUrlOrHost(): Boolean {
  // 只判断 host(长度1~63,结尾是.然后带2~6个字符如[.com]，没有端口判断)：val regex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}\$".toRegex()
  // 以 http 或者 https 或者 ftp 打头，可以没有
  // 字符串中只能包含数字和字母，同时可以存在-
  // 最后以 2~5个字符 结尾，可能还存在端口信息，端口信息限制数字，长度为1~5位
  val regex =
    "^((https?|ftp)://)?([a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,5}(:[0-9]{1,5})?(/.*)?)$".toRegex()
  return regex.matches(this)
}

internal fun String.toRequestUrl(): String {
  return if (this.startsWith("http://") || this.startsWith("https://") || this.startsWith("ftp://")) {
    this
  } else {
    "https://$this"
  }
}

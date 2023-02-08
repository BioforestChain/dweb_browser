package info.bagen.rust.plaoc.microService

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import info.bagen.rust.plaoc.webView.api.BFSApi
import info.bagen.rust.plaoc.webView.bottombar.BottomBarState
import info.bagen.rust.plaoc.webView.bottombar.DWebBottomBar
import info.bagen.rust.plaoc.webView.dialog.*
import info.bagen.rust.plaoc.webView.jsutil.JsUtil
import info.bagen.rust.plaoc.webView.network.*
import info.bagen.rust.plaoc.webView.openDWebWindow
import info.bagen.rust.plaoc.webView.systemui.SystemUIState
import info.bagen.rust.plaoc.webView.systemui.js.VirtualKeyboardFFI
import info.bagen.rust.plaoc.webView.topbar.DWebTopBar
import info.bagen.rust.plaoc.webView.topbar.TopBarState
import info.bagen.rust.plaoc.webView.urlscheme.CustomUrlScheme
import info.bagen.rust.plaoc.webkit.*
import java.net.URI
import kotlin.math.min




private const val LEAVE_URI_SYMBOL = ":~:dweb=leave"

@ExperimentalLayoutApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DWebView(
    state: AdWebViewState,
    navController: NavController,
    activity: ComponentActivity,
    customUrlScheme: CustomUrlScheme,
    modifier: Modifier = Modifier,
    onCreated: (AdAndroidWebView) -> Unit = {},
) {

    var jsUtil by remember(state) { mutableStateOf<JsUtil?>(null) }
    val hook = remember { AdWebViewHook() }
    val adNavigator = rememberAdWebViewNavigator()
    val adCaptureBackPresses by remember { mutableStateOf(true) }
    val doBack = state.content.getCurrentUrl()?.endsWith(LEAVE_URI_SYMBOL) == true

    BackHandler(
        // 如果要执行doBack，那么要禁用拦截
        doBack
                // 如果有js上下文
                and
                (jsUtil != null)
                // 并且没有历史记录了，说明返回按钮会触发"退出行为"
                and
                !(adCaptureBackPresses && adNavigator.canGoBack)
    ) {
        // 这种 location.replace 行为不会触发 Navigator 的长度发生变化的同时，还能自动触发 onbeforeunload
        // @TODO 这里的风险在于，如果js代码卡住，那么这段代码会无法正常执行，那么就永远无法退出
        jsUtil!!.evalQueue("leave_page") { "location.replace(location.href+'#$LEAVE_URI_SYMBOL')" }
    }
    if (doBack) {
        SideEffect {
            activity.onBackPressed()
        }
    }

    SetTaskDescription(state, activity)

    val systemUIState = SystemUIState.Default(activity)

    jsUtil?.apply {
        VirtualKeyboardFFI.injectVirtualKeyboardVars(
            this,
            LocalDensity.current, LocalLayoutDirection.current,
            systemUIState.virtualKeyboard.overlay.value, WindowInsets.ime,
            systemUIState.navigationBar.overlay.value, WindowInsets.navigationBars,
        )
    }

    var overlayOffset = IntOffset(0, 0)
    val overlayPadding = WindowInsets(0).let {
        var res = it
        if (!systemUIState.statusBar.overlay.value) {
            res = res.add(WindowInsets.statusBars)
        }

        if (!systemUIState.virtualKeyboard.overlay.value && WindowInsets.isImeVisible) {
            // it.add(WindowInsets.ime) // ime本身就包含了navigationBars的高度
            overlayOffset =
                IntOffset(
                    0, min(
                        0, -WindowInsets.ime.getBottom(LocalDensity.current)
                                + WindowInsets.navigationBars.getBottom(LocalDensity.current)
                    )
                )
        } else if (!systemUIState.navigationBar.overlay.value) {
            res = res.add(WindowInsets.navigationBars)
        }
        res
    }.asPaddingValues()

    // Log.i(TAG, "overlayPadding:$overlayPadding")
    val pressBack = remember {
        return@remember {
            activity.runOnUiThread {
                activity.onBackPressed()
            }
        }
    }
    val topBarState = TopBarState.Default(pressBack)

    @Composable
    fun TopAppBar() {
        DWebTopBar(jsUtil, state, topBarState)
    }

    val bottomBarState = BottomBarState.Default()

    @Composable
    fun BottomAppBar() {
        DWebBottomBar(jsUtil, bottomBarState)
    }

    Scaffold(
        modifier = modifier
            .padding(overlayPadding)
            .offset { overlayOffset },
        // 如果前端没有传递overlay,并且没有传递enabled
        topBar = { if (topBarState.enabled.value) TopAppBar() },
        bottomBar = {
            // Log.i("DwebView","bottomBarState.isEnabled:${ bottomBarState.isEnabled}, bottomBarState.overlay:${ bottomBarState.overlay.value}");
            // 如果前端没有传递hidden，也就是bottomBarState.isEnabled等于true，则显示bottom bar
            if (bottomBarState.isEnabled) {
                BottomAppBar()
            }
        },
        content = { innerPadding ->
            val jsAlertConfig = remember { mutableStateOf<JsAlertConfiguration?>(null) }
            val jsPromptConfig = remember { mutableStateOf<JsPromptConfiguration?>(null) }
            val jsConfirmConfig = remember { mutableStateOf<JsConfirmConfiguration?>(null) }
            val jsWarningConfig = remember { mutableStateOf<JsConfirmConfiguration?>(null) }

            AdWebView(
                state = state,
                navigator = adNavigator,
                captureBackPresses = adCaptureBackPresses,
                onCreated = { webView ->
                    // 将webView的背景默认设置为透明。不通过systemUi的api提供这个功能，一些手机上动态地修改webView背景颜色，在黑夜模式下，会有问题
                    webView.setBackgroundColor(Companion.Transparent.toArgb())
                    // 设置debugging
                    WebView.setWebContentsDebuggingEnabled(true)
                    webView.adWebViewHook = hook
                    onCreated(webView)
                    webView.addJavascriptInterface(BFSApi(), "bfs") // 注入bfs，js可以直接调用
                },
                chromeClient = remember {
                    class MyWebChromeClient : AdWebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            if (view != null) {
                                val href = view.handler.obtainMessage()
                                view.requestFocusNodeHref(href)
                                val url = href.data.getString("url")
                                if (url != null) {
                                    openDWebWindow(activity = activity, url = url)
                                    return true
                                }
                            }
                            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                        }

                        private fun getJsDialogTitle(url: String?, label: String): String {
                            var title = state.pageTitle
                            try {
                                if (url != null) {
                                    val host = URI(url).host
                                    if (host.isNotEmpty()) {
                                        title = host
                                    }
                                }
                            } catch (_: Exception) {
                            }
                            return "$title $label"
                        }

                        private fun getJsDialogConfirmText() = "确认"
                        private fun getJsDialogCancelText() = "取消"

                        override fun onJsAlert(
                            view: WebView?, url: String?, message: String?, result: JsResult?
                        ): Boolean {
                            Log.e("DWebView", "chromeClient::onJsAlert url=$url, message=$message, result=$result")
                            if (result == null) {
                                return super.onJsAlert(view, url, message, result)
                            }
                            jsAlertConfig.value = JsAlertConfiguration(
                                getJsDialogTitle(url, "显示"),
                                message ?: "",
                                getJsDialogConfirmText(),
                            ).bindCallback(result)
                            return true
                        }

                        override fun onJsPrompt(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            defaultValue: String?,
                            result: JsPromptResult?
                        ): Boolean {
                            Log.e("DWebView", "chromeClient::onJsPrompt url=$url, message=$message, result=$result")
                            if (result == null) {
                                return super.onJsPrompt(view, url, message, defaultValue, result)
                            }
                            jsPromptConfig.value = JsPromptConfiguration(
                                getJsDialogTitle(url, "填入"),
                                message ?: "",
                                defaultValue ?: "",
                                getJsDialogConfirmText(),
                                getJsDialogCancelText(),
                            ).bindCallback(result)
                            return true
                        }

                        override fun onJsConfirm(
                            view: WebView?, url: String?, message: String?, result: JsResult?
                        ): Boolean {
                            Log.e("DWebView", "chromeClient::onJsConfirm url=$url, message=$message, result=$result")
                            if (result == null) {
                                return super.onJsConfirm(view, url, message, result)
                            }
                            jsConfirmConfig.value = JsConfirmConfiguration(
                                getJsDialogTitle(url, "询问"),
                                message ?: "",
                                getJsDialogConfirmText(),
                                getJsDialogCancelText(),
                            ).bindCallback(result)
                            return true
                        }

                        override fun onJsBeforeUnload(
                            view: WebView?, url: String?, message: String?, result: JsResult?
                        ): Boolean {
                            Log.e("DWebView", "chromeClient::onJsBeforeUnload url=$url, message=$message, result=$result")
                            if (result == null) {
                                return super.onJsBeforeUnload(view, url, message, result)
                            }
                            jsWarningConfig.value = JsConfirmConfiguration(
                                getJsDialogTitle(url, "提示您"),
                                message ?: "",
                                "离开",
                                "留下",
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true,
                            ).bindCallback(result)
                            return true
                        }

                    }
                    MyWebChromeClient()
                },
                client = remember {
                    val swController = ServiceWorkerController.getInstance()
                    swController.setServiceWorkerClient(object : ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                            println("kotlin#DWebView shouldInterceptRequest request=${request.url}")
                            // 拦截serviceWorker的网络请求
                            val result =  interceptNetworkRequests(request, customUrlScheme)
                            if (result != null) {
                                return result
                            }
                            return super.shouldInterceptRequest(request)
                        }
                    })
                    swController.serviceWorkerWebSettings.allowContentAccess = true
                    class MyWebViewClient : AdWebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.let {
                                // 为每一个webWorker都创建一个通道
                                val channel = view.createWebMessageChannel()
                                channel[0].setWebMessageCallback(object :
                                    WebMessagePort.WebMessageCallback() {
                                    override fun onMessage(port: WebMessagePort, message: WebMessage) {
                                        println("kotlin#DwebView🍑message: ${message.data}")
                                    }
                                })
                                // 发送post1到service worker层 建立通信
                                view.postWebMessage(WebMessage("forward-to-service-worker", arrayOf(channel[1])), Uri.EMPTY)
                                channel[0].postMessage(WebMessage("xxx"))
                            }
                        }
                        // API >= 21
                        @SuppressLint("NewApi")
                        @Override
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
//                            return super.shouldInterceptRequest(view, request)
                            request?.let { webResourceRequest ->
                                val url = webResourceRequest.url
                                val path = url.path.toString()
                               println("kotlin#DWebView shouldInterceptRequest url=$url, path=$path, url.host=${url.host}")
                                if (url.host?.endsWith("sys.dweb") == true) {
                                    // 这里出来的url全部都用是小写，serviceWorker没办法一开始就注册，所以还会走一次这里
                                    return interceptNetworkRequests(request, customUrlScheme)
                                }
                            }
                            return null
                        }
                    }
                    MyWebViewClient()
                },
                modifier = Modifier.let { m ->
                    var top = innerPadding.calculateTopPadding()
                    var bottom = innerPadding.calculateBottomPadding()
                    val layoutDirection = LocalLayoutDirection.current
                    var start = innerPadding.calculateStartPadding(layoutDirection)
                    var end = innerPadding.calculateEndPadding(layoutDirection)
                    if ((topBarState.alpha.value != 1F) or !topBarState.enabled.value) {
                        top = 0.dp
                    }
                    // 如果不显示bottomBar，即bottomBarState.isEnabled 为false
                    if ((bottomBarState.alpha.value != 1F) or !bottomBarState.isEnabled) {
                        bottom = 0.dp
                    }
                    if ((top.value == 0F) and (bottom.value == 0F)) {
                        start = 0.dp; end = 0.dp
                    }
                    m.padding(start, top, end, bottom)
                },
            )

            // 如果前端传递了透明度属性，并且是需要显示的
            if ((topBarState.alpha.value != 1F) and topBarState.enabled.value) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = if (systemUIState.statusBar.overlay.value) {
                        with(LocalDensity.current) {
                            Modifier.offset(y = WindowInsets.statusBars.getTop(this).toDp())
                        }
                    } else {
                        Modifier
                    }
                ) {
                    TopAppBar()
                }
            }
            if ((bottomBarState.alpha.value != 1F) and bottomBarState.isEnabled) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.fillMaxSize()
                        .let {
                            it
                        }
                ) {
                    BottomAppBar()
                }
            }

            jsAlertConfig.value?.openAlertDialog { jsAlertConfig.value = null }
            jsPromptConfig.value?.openPromptDialog { jsPromptConfig.value = null }
            jsConfirmConfig.value?.openConfirmDialog { jsConfirmConfig.value = null }
            jsWarningConfig.value?.openWarningDialog { jsWarningConfig.value = null }
        },
        containerColor = Companion.Transparent,
        contentColor = Companion.Transparent
    )
}

@Composable
fun SetTaskDescription(state: AdWebViewState, activity: ComponentActivity) {
    var pageTitle by remember {
        mutableStateOf(state.pageTitle)
    }
    var pageIcon by remember {
        mutableStateOf(state.pageIcon)
    }
    if (pageTitle != state.pageTitle || pageIcon != state.pageIcon) {
        pageTitle = state.pageTitle
        pageIcon = state.pageIcon
        activity.runOnUiThread {
            activity.setTaskDescription(
                ActivityManager.TaskDescription(
                    pageTitle, pageIcon
                )
            )
        }
    }
}

package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.webkit.JsResult
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.WebView
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger


open class PermissionActivity : AppCompatActivity() {
    companion object {
        val PERMISSION_REQUEST_CODE_PHOTO = 2
        private val requestPermissionsResultMap = mutableMapOf<Int, RequestPermissionsResult>()
        private var requestPermissionsCodeAcc = AtomicInteger(1);
    }

    class RequestPermissionsResult(val code: Int) {
        val grants = mutableListOf<String>()
        val denied = mutableListOf<String>()
        private val task = PromiseOut<Unit>()
        fun done() {
            task.resolve(Unit)
        }

        val isGranted get() = denied.size == 0

        suspend fun waitPromise() = task.waitPromise()
    }

    suspend fun requestPermissions(permissions: Array<String>): RequestPermissionsResult {
        val result = RequestPermissionsResult(requestPermissionsCodeAcc.getAndAdd(1))

        val shouldRequestPermissions = permissions
//            permissions.filter {
//                ActivityCompat.shouldShowRequestPermissionRationale(this, it).also { isDenied ->
//                    if (isDenied) {
//                        result.denied.add(it)
//                    }
//                }
//            }.toTypedArray()
        if (shouldRequestPermissions.isNotEmpty()) {
            requestPermissionsResultMap[result.code] = result
            runOnUiThread {
                ActivityCompat.requestPermissions(
                    this, shouldRequestPermissions, result.code
                )
            }
        } else {
            result.done()
        }

        result.waitPromise()
        return result
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        requestPermissionsResultMap.remove(requestCode)?.also { result ->
            grantResults.forEachIndexed { index, p ->
                if (p == PackageManager.PERMISSION_GRANTED) {
                    result.grants.add(permissions[index])
                } else {
                    result.denied.add(permissions[index])
                }
            }
            result.done()
        }
    }
}

open class MultiWebViewActivity : PermissionActivity() {


    private var remoteMmid by mutableStateOf("")
    private var controller: MultiWebViewController? = null

    private fun upsetRemoteMmid() {
        remoteMmid = intent.getStringExtra("mmid") ?: return finish()
        controller?.activity = null

        controller = MultiWebViewNMM.controllerMap[remoteMmid]?.also { controller ->
            controller.activity = this
            controller.onWebViewClose {
                // 如果webview实例都销毁完了，那就关闭自己
                if (controller.webViewList.size == 0) {
                    finish()
                }
            }
        } ?: throw Exception("no found controller by mmid:$remoteMmid")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 选中图片
        if (requestCode == PERMISSION_REQUEST_CODE_PHOTO) {
            controller?.webViewList?.last()?.webView?.also { webview ->
                webview.filePathCallback?.also {
                    it.onReceiveValue(
                        if (resultCode == RESULT_OK) arrayOf(data?.data!!) else emptyArray()
                    )
                    webview.filePathCallback = null
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        upsetRemoteMmid()
    }

    override fun onRestart() {
        super.onRestart()
        upsetRemoteMmid()

    }


    @OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()

        // This will lay out our app behind the system bars
//        WindowCompat.setDecorFitsSystemWindows(window, false)

//        val metrics = WindowMetricsCalculator.getOrCreate()
//            .computeCurrentWindowMetrics(this)
//        val widthDp = (metrics.bounds.width() /
//            resources.displayMetrics.density).dp
//        val heightDp = (metrics.bounds.height() /
//            resources.displayMetrics.density).dp
//

        setContent {
//
//            val systemUiController = rememberSystemUiController()
//            val useDarkIcons = !isSystemInDarkTheme()
//            DisposableEffect(systemUiController, useDarkIcons) {
//                // 更新所有系统栏的颜色为透明
//                // 如果我们在浅色主题中使用深色图标
//                systemUiController.setSystemBarsColor(
//                    color = Color.Transparent,
//                    darkIcons = useDarkIcons,
//                )
//
//                onDispose {}
//            }


            RustApplicationTheme {

                val wc by remember(remoteMmid) {
                    mutableStateOf(
                        controller ?: throw Exception("no found controller")
                    )
                }
//
//                val nativeUiController = NativeUiController.remember(this)
//
//                val modifierOffset by nativeUiController.modifierOffsetState
//                val modifierPadding by nativeUiController.modifierPaddingState
//                val modifierScale by nativeUiController.modifierScaleState

                wc.webViewList.forEachIndexed { index, viewItem ->
                    key(viewItem.webviewId) {
                        val nativeUiController = viewItem.nativeUiController.effect()

                        val state = viewItem.state
                        val navigator = viewItem.navigator
                        var beforeUnloadPrompt by remember { mutableStateOf("") }
                        var beforeUnloadResult by remember { mutableStateOf<JsResult?>(null) }
                        BackHandler(true) {
                            if (navigator.canGoBack) {
                                debugMultiWebView("NAV/${viewItem.webviewId}", "go back")
                                navigator.navigateBack()
                            } else {
                                wc.closeWebView(viewItem.webviewId)
                            }
                        }

                        val keyboardController = LocalSoftwareKeyboardController.current
                        var (text, setText) = remember {
                            mutableStateOf("Close keyboard on done ime action (blue ✔️)")
                        }
                        val chromeClient = remember {
                            object : AccompanistWebChromeClient() {
                                override fun onJsBeforeUnload(
                                    view: WebView, url: String, message: String, result: JsResult
                                ): Boolean {
                                    debugMultiWebView(
                                        "onJsBeforeUnload", "url:$url message:$message"
                                    )
                                    if (message.isNotEmpty()) {
                                        if (index == wc.webViewList.size - 1) {
                                            beforeUnloadPrompt = message
                                            beforeUnloadResult = result
                                        } else {
                                            result.cancel()
                                        }
                                        return true
                                    }
                                    return super.onJsBeforeUnload(view, url, message, result)
                                }

                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message
                                ): Boolean {
                                    val transport = resultMsg.obj;
                                    if (transport is WebView.WebViewTransport) {
                                        viewItem.coroutineScope.launch {
                                            debugMultiWebView("opening")
                                            val dWebView = wc.createDwebView("")
                                            transport.webView = dWebView;
                                            resultMsg.sendToTarget();

                                            // 它是有内部链接的，所以等到它ok了再说
                                            var url = dWebView.getUrlInMain()
                                            if (url?.isEmpty() != true) {
                                                val readyPo = PromiseOut<Unit>()
                                                dWebView.onReady { readyPo.resolve(Unit) }
                                                readyPo.waitPromise()
                                                url = dWebView.getUrlInMain()
                                            }
                                            debugMultiWebView("opened", url)
                                            wc.appendWebViewAsItem(dWebView)
                                        }
                                        return true
                                    }

                                    return super.onCreateWindow(
                                        view, isDialog, isUserGesture, resultMsg
                                    )
                                }

                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.Blue)
                                .fillMaxSize()
                        ) {
                            BasicTextField(
                                text,
                                setText,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                    keyboardType = KeyboardType.Decimal
                                ),
                                keyboardActions = KeyboardActions(
//                                    onDone = { keyboardController?.hide() }
                                ),
                                modifier = Modifier
                                    .focusRequester(nativeUiController.virtualKeyboard.focusRequester)
                                    .fillMaxWidth()
                                    .zIndex(-1.0F)
                            )
                            val modifierPadding by nativeUiController.safeArea.outerAreaInsetsState
                            WebView(
                                state = state,
                                navigator = navigator,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(nativeUiController.virtualKeyboard.focusRequester)
                                    .padding(modifierPadding.asPaddingValues()),
                                factory = { viewItem.webView },
                                chromeClient = chromeClient,
                            )

                            beforeUnloadResult?.also { jsResult ->
                                AlertDialog( //
                                    title = { Text("确定要离开吗？") },
                                    text = { Text(beforeUnloadPrompt) },
                                    onDismissRequest = {
                                        jsResult.cancel()
                                        beforeUnloadResult = null
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            jsResult.confirm()
                                            beforeUnloadResult = null
                                            wc.closeWebView(viewItem.webviewId)
                                        }) {
                                            Text("确定")
                                        }
                                    },
                                    dismissButton = {
                                        Button(onClick = {
                                            jsResult.cancel()
                                            beforeUnloadResult = null
                                        }) {
                                            Text("留下")
                                        }
                                    })
                            }

//                            DebugPanel(viewItem)
                        }

                    }
                }

            }
        }
    }

    @Composable
    fun DebugPanel(viewItem: MultiWebViewController.ViewItem) {
        val nativeUiController = viewItem.nativeUiController.effect()

        Column(
            modifier = Modifier.padding(top = 30.dp, start = 20.dp),
        ) {
            @Composable
            fun RowSwitchItem(
                text: String, switchState: MutableState<Boolean>
            ) {
                var switch by switchState
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                        .toggleable(
                            value = switch, onValueChange = {
                                switch = it
                            }, role = Role.Switch
                        )
                ) {
                    Switch(
                        checked = switch, onCheckedChange = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        text = text
                    )
                }
            }
            RowSwitchItem(
                "statusBarOverlay", nativeUiController.statusBar.overlayState
            )
            RowSwitchItem(
                "virtualKeyboardOverlay", nativeUiController.virtualKeyboard.overlayState
            )
            RowSwitchItem(
                "navigationBarOverlay", nativeUiController.navigationBar.overlayState
            )

        }
    }
}


class MultiWebViewPlaceholder1Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder2Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder3Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder4Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder5Activity : MultiWebViewActivity()

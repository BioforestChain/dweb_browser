package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.webkit.JsResult
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.WebView
import com.google.common.primitives.Ints.min
import info.bagen.rust.plaoc.microService.browser.*
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUIState
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUiPlugin
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.util.rememberIsChange
import kotlinx.coroutines.launch


open class PermissionActivity : AppCompatActivity() {

    companion object {
        private val requestPermissionsResultMap = mutableMapOf<Int, RequestPermissionsResult>()
        private var requestPermissionsCodeAcc = 1;
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
        val result = RequestPermissionsResult(requestPermissionsCodeAcc++)

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

open class MutilWebViewActivity : PermissionActivity() {


    private var remoteMmid by mutableStateOf("")
    private var controller: MutilWebViewController? = null

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

    override fun onResume() {
        super.onResume()
        upsetRemoteMmid()
    }

    override fun onRestart() {
        super.onRestart()
        upsetRemoteMmid()

    }


    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()

        // This will lay out our app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val systemUiController = rememberSystemUiController()
            systemUiController.setSystemBarsColor(Color.Transparent)


            RustApplicationTheme {

                val wc by remember(remoteMmid) {
                    mutableStateOf(
                        controller ?: throw Exception("no found controller")
                    )
                }

                val systemUIState = SystemUIState.Default(this)

                var overlayOffset by remember { mutableStateOf(IntOffset(0, 0)) }
//                var overlayPadding by remember { mutableStateOf(PaddingValues()) }
//                val isSystemUILayoutChanged = rememberIsChange()
//                isSystemUILayoutChanged.rememberStateOf(systemUIState.virtualKeyboard.overlay)
//                isSystemUILayoutChanged.rememberStateOf(systemUIState.statusBar.overlay)
//                isSystemUILayoutChanged.rememberStateOf(WindowInsets.isImeVisible)
//                isSystemUILayoutChanged.rememberStateOf(systemUIState.navigationBar.overlay)
//                isSystemUILayoutChanged.rememberStateOf(WindowInsets.navigationBars)
//                isSystemUILayoutChanged.effectChange {
//
//                }
               val overlayPadding = WindowInsets(0).let {
                    var res = it
                    if (!systemUIState.statusBar.overlay.value) {
                        res = res.add(WindowInsets.statusBars)
                    }
                    if (!systemUIState.virtualKeyboard.overlay.value && WindowInsets.isImeVisible) {
                        // it.add(WindowInsets.ime) // ime本身就包含了navigationBars的高度
                        overlayOffset = IntOffset(
                            0, min(
                                0,
                                WindowInsets.navigationBars.getBottom(LocalDensity.current) - WindowInsets.ime.getBottom(
                                    LocalDensity.current
                                )
                            )
                        )
                    } else if (!systemUIState.navigationBar.overlay.value) {
                        res = res.add(WindowInsets.navigationBars)
                    }
                    res
                }.asPaddingValues()


                wc.webViewList.forEachIndexed { index, viewItem ->
                    println("viewItem.webviewId: ${viewItem.webviewId}")
                    key(viewItem.webviewId) {
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
                        viewItem.systemUiPlugin = SystemUiPlugin(viewItem.webView, systemUIState)

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
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(overlayPadding)
                            .offset { overlayOffset }) {
                            WebView(
                                state = state,
                                navigator = navigator,
                                modifier = Modifier.fillMaxSize(),
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
                        }

                    }
                }

            }
        }
    }
}


class MutilWebViewPlaceholder1Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder2Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder3Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder4Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder5Activity : MutilWebViewActivity()
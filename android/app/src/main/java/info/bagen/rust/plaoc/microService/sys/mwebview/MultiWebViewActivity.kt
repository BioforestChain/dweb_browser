package info.bagen.rust.plaoc.microService.sys.mwebview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.accompanist.web.WebView
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
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
                if (controller.lastViewOrNull == null) {
                    finish()
                }
            }
        } ?: throw Exception("no found controller by mmid:$remoteMmid")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 选中图片
        if (requestCode == PERMISSION_REQUEST_CODE_PHOTO) {
            controller?.lastViewOrNull?.also { (_, webview) ->
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

                val wc = rememberViewController()
//
//                val nativeUiController = NativeUiController.remember(this)
//
//                val modifierOffset by nativeUiController.modifierOffsetState
//                val modifierPadding by nativeUiController.modifierPaddingState
//                val modifierScale by nativeUiController.modifierScaleState

                wc.eachView { viewItem ->
                    key(viewItem.webviewId) {
                        val nativeUiController = viewItem.nativeUiController.effect()

                        val state = viewItem.state
                        val navigator = viewItem.navigator
                        BackHandler(true) {
                            if (navigator.canGoBack) {
                                debugMultiWebView("NAV/${viewItem.webviewId}", "go back")
                                navigator.navigateBack()
                            } else {
                                wc.closeWebView(viewItem.webviewId)
                            }
                        }

                        rememberCoroutineScope()
                        val chromeClient = remember {
                            MutilWebViewChromeClient(
                                wc,
                                viewItem,
                                wc.isLastView(viewItem)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.Blue)
                                .fillMaxSize()
                        ) {
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

                            chromeClient.beforeUnloadDialog()

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

    @SuppressLint("RememberReturnType")
    @Composable
    fun rememberViewController(): MultiWebViewController {
        return remember(remoteMmid) {
            controller ?: throw Exception("no found controller")
        }
    }


}


class MultiWebViewPlaceholder1Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder2Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder3Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder4Activity : MultiWebViewActivity()
class MultiWebViewPlaceholder5Activity : MultiWebViewActivity()

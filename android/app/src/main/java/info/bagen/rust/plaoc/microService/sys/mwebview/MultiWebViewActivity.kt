package info.bagen.rust.plaoc.microService.sys.mwebview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.accompanist.web.WebView
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM.Companion.controllerMap
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger


open class PermissionActivity : ComponentActivity() {
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

        if (permissions.isNotEmpty()) {
            requestPermissionsResultMap[result.code] = result
            runOnUiThread {
                ActivityCompat.requestPermissions(
                    this, permissions, result.code
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

        controller = controllerMap[remoteMmid]?.also { controller ->
            controller.activity = this
            controller.onWebViewClose {
                // 如果webview实例都销毁完了，那就关闭自己
                if (controller.lastViewOrNull == null) {
                    finish()
                }
            }
        } ?: throw Exception("no found controller by mmid:$remoteMmid")
    }

    val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultCode: Int = result.resultCode
            val data: Intent? = result.data
            // 选中图片
            controller?.lastViewOrNull?.also { (_, webview) ->
                if (resultCode == RESULT_OK) {
                    val uris = data?.clipData?.let { clipData ->
                        val count = clipData.itemCount
                        val uris = ArrayList<Uri>()
                        for (i in 0 until count) {
                            clipData.getItemAt(i)?.uri?.let { uri ->
                                uris.add(uri)
                            }
                        }
                        uris.toTypedArray()
                    } ?: arrayOf(data?.data!!)
                    // 调用回调函数，将Uri数组传递给WebView
                    webview.filePathCallback?.onReceiveValue(uris)
                } else {
                    // 取消选择文件操作，调用回调函数并传递null值
                    webview.filePathCallback?.onReceiveValue(null)
                }
                webview.filePathCallback = null

            }
        }

    override fun onDestroy() {
        super.onDestroy()
        controller?.destroyWebView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()
        setContent {
            RustApplicationTheme {
                val wc = rememberViewController()
                wc.eachView { viewItem ->
                    key(viewItem.webviewId) {
                        val nativeUiController = viewItem.nativeUiController.effect()

                        val state = viewItem.state
                        val navigator = viewItem.navigator

                        val chromeClient = remember {
                            MutilWebViewChromeClient(
                                wc,
                                viewItem,
                                wc.isLastView(viewItem)
                            )
                        }

                        BackHandler(true) {
                            if (chromeClient.closeWatcherController.canClose) {
                                viewItem.coroutineScope.launch {
                                    chromeClient.closeWatcherController.close()
                                }
                            } else if (navigator.canGoBack) {
                                debugMultiWebView("NAV/${viewItem.webviewId}", "go back")
                                navigator.navigateBack()
                            } else {
                                viewItem.coroutineScope.launch {
                                    wc.closeWebView(viewItem.webviewId)
                                }
                            }
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
                                factory = {
                                    // 修复 activity 已存在父级时导致的异常
                                    viewItem.webView.parent?.let { parentView ->
                                        (parentView as ViewGroup).removeAllViews()
                                    }
                                    viewItem.webView
                                },
                                chromeClient = chromeClient,
                                captureBackPresses = false,
                            )
                            chromeClient.beforeUnloadDialog()
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
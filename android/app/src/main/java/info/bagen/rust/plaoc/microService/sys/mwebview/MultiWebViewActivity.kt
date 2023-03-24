package info.bagen.rust.plaoc.microService.sys.mwebview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.toBitmap
import info.bagen.rust.plaoc.microService.sys.plugin.camera.CameraPlugin.Companion.REQUEST_CAMERA_IMAGE
import info.bagen.rust.plaoc.microService.sys.plugin.camera.CameraPlugin.Companion.REQUEST_IMAGE_CAPTURE
import info.bagen.rust.plaoc.microService.sys.plugin.camera.debugCameraNMM
import info.bagen.rust.plaoc.microService.sys.plugin.share.debugShare
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger


open class PermissionActivity : AppCompatActivity() {
    companion object {
        val PERMISSION_REQUEST_CODE_PHOTO = 2
        val RESULT_SHARE_CODE = 3
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
        // 选中照片返回数据
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            val imageData = data.data?.toBitmap(contentResolver)
            GlobalScope.launch(ioAsyncExceptionHandler) {
                controller?.getPhotoSignal?.emit(imageData)
                debugCameraNMM("REQUEST_IMAGE_CAPTURE", imageData)
            }
        }
        // 拍照返回数据处理
        if (requestCode == REQUEST_CAMERA_IMAGE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            GlobalScope.launch(ioAsyncExceptionHandler) {
                controller?.getCameraSignal?.emit(imageBitmap)
                debugCameraNMM("REQUEST_CAMERA_IMAGE", imageBitmap)
            }
        }
        // 分享返回数据
        if (requestCode == RESULT_SHARE_CODE) {
            GlobalScope.launch(ioAsyncExceptionHandler) {
                controller?.getShareSignal?.emit(data?.dataString ?: "OK")
                debugShare("RESULT_SHARE_CODE", data?.dataString)
            }
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
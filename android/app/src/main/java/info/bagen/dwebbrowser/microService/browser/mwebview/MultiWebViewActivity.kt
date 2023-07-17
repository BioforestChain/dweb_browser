package info.bagen.dwebbrowser.microService.browser.mwebview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.base.BaseActivity
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewNMM.Companion.controllerMap
import info.bagen.dwebbrowser.microService.browser.mwebview.dwebServiceWorker.ServiceWorkerEvent
import info.bagen.dwebbrowser.microService.browser.mwebview.dwebServiceWorker.emitEvent
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class MultiWebViewActivity : BaseActivity() {
    private var remoteMmid by mutableStateOf("")
    private var controller: MultiWebViewController? = null

    private fun upsetRemoteMmid() {
        remoteMmid = intent.getStringExtra("mmid") ?: return finish()
        controller?.activity = null

        controller = controllerMap[remoteMmid]?.also { controller ->
            controller.activity = this
            controller.onWebViewClose {
                // 如果webView实例都销毁完了，那就关闭自己
                if (controller.lastViewOrNull == null) {
                    finish()
                }
            }
        } ?: throw Exception("no found controller by mmid:$remoteMmid")
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(ioAsyncExceptionHandler) {
            emitEvent(remoteMmid, ServiceWorkerEvent.Resume.event)
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(ioAsyncExceptionHandler) {
            emitEvent(remoteMmid, ServiceWorkerEvent.Pause.event, """new Event("pause")""")
        }
    }

    override fun onDestroy() {
        controllerMap.remove(remoteMmid)?.let {
            GlobalScope.launch (ioAsyncExceptionHandler){
                it.destroyWebView()
            }
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()
        setContent {
            DwebBrowserAppTheme {
                val wc = rememberViewController()
                wc.eachView { viewItem ->
                    key(viewItem.webviewId) {
                        val nativeUiController = viewItem.nativeUiController.effect()
                        val state = viewItem.state
                        val navigator = viewItem.navigator

                        val chromeClient = remember {
                            MultiWebViewChromeClient(
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
                            modifier = Modifier.fillMaxSize()
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
    fun DebugPanel(viewItem: MultiWebViewController.MultiViewItem) {
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
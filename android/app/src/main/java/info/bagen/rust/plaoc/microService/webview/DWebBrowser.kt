package info.bagen.rust.plaoc.microService.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUIState
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUiPlugin
import info.bagen.rust.plaoc.webkit.AdWebViewHook
import info.bagen.rust.plaoc.webkit.CustomMenu

private val enterAnimator = slideInHorizontally(
    animationSpec = tween(500),//动画时长1s
    initialOffsetX = {
        it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
    }
)
private val exitAnimator = slideOutHorizontally(
    animationSpec = tween(500),//动画时长1s
    targetOffsetX = {
        it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
    }
)


@Composable
fun MultiDWebBrowserView(dWebBrowserModel: DWebBrowserModel) {

    AnimatedVisibility(
        visible = dWebBrowserModel.uiState.show.value, enter = enterAnimator, exit = exitAnimator
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            for (item in dWebBrowserModel.uiState.dWebBrowserList) {
                DWebBrowserView(dWebBrowserModel, item)
            }
        }
    }
}

@Composable
fun DWebBrowserView(dWebBrowserModel: DWebBrowserModel, item: DWebBrowserItem) {
    AnimatedVisibility(
        visible = item.show.value, enter = enterAnimator, exit = exitAnimator
    ) {
        val hook = remember { AdWebViewHook() }
        val systemUIState = App.browserActivity?.let { SystemUIState.Default(it) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            BackHandler { dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveLast) }
            AndroidView(factory = {
                item.dWebBrowser.adWebViewHook = hook
                if (systemUIState != null) {
                    item.systemUi = SystemUiPlugin(item.dWebBrowser.mWebView, hook, systemUIState)
                }
                item.dWebBrowser
            })
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
class DWebBrowser(context: Context, url: String) : FrameLayout(context) {
    var mWebView: WebView

    var adWebViewHook: AdWebViewHook? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return adWebViewHook?.onTouchEvent?.let { it(event) } ?: super.onTouchEvent(event)
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode {
        val myCallback = object : ActionMode.Callback2() {
            var customMenu: CustomMenu? = null

            /** 报告用户单击操作按钮。*/
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                println("2==============onActionItemClicked===" + item.title)
                var res = false

                res = if (this.customMenu == null) {
                    callback?.onActionItemClicked(mode, item) ?: res
                } else {
                    true
                }
                return res
            }

            /** 首次创建动作模式时调用。 提供的菜单将用于为动作模式生成动作按钮。*/
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val res = adWebViewHook?.onCreateMenu?.let {
                    val customMenu = it(mode, menu)
                    this.customMenu = customMenu
                    mode.title = customMenu.title
                    mode.subtitle = customMenu.subtitle
                    true
                } ?: callback?.onCreateActionMode(mode, menu) ?: true
                println("2==============onCreateActionMode===" + menu.size())
                return res
            }

            /** 调用以在操作模式无效时刷新操作模式的操作菜单*/
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val res = this.customMenu?.let {
                    // @TODO 这里通过SystemUIFFI来实现自定义菜单
                    for (m in it.menus) {
                        menu.add(m.key).setOnMenuItemClickListener {
                            mWebView.evaluateJavascript(m.value) {
                                // @TODO 这里可以通过回调进一步对菜单栏做操作，这里只是简单地将之关闭
                                mode.finish()
                            }
                            true
                        }
                    }

                    true

                } ?: callback?.onPrepareActionMode(mode, menu) ?: true
                println("2==============onPrepared===" + menu.size())

                return res
            }

            /** */
            override fun onDestroyActionMode(mode: ActionMode) {
                this.customMenu = null
                println("2==============onDestroyActionMode===" + mode.title)
                callback?.onDestroyActionMode(mode)
            }

            override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
                try {
                    (callback as ActionMode.Callback2).onGetContentRect(mode, view, outRect)
                } catch (e: Throwable) {
                    super.onGetContentRect(mode, view, outRect)
                }
            }

        }
        return super.startActionMode(myCallback, type)
    }


    init {
        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        this.mWebView = WebView(context).apply {
            this.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = true
                domStorageEnabled = true
            }

            this.webViewClient = DWebBrowserViewClient()
            this.webChromeClient = DWebBrowserChromeClient()
            loadUrl(url)
        }

        this.addView(this.mWebView)
    }

    fun canGoBack() = mWebView.canGoBack()
    fun goBack() = mWebView.goBack()
    fun destroy() = mWebView.destroy()
}
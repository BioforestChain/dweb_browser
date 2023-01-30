package info.bagen.rust.plaoc.webkit

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.webkit.WebView

private const val TAG = "AdAndroidWebView"

class AdAndroidWebView(context: Context) : WebView(context) {
    var adWebViewHook: AdWebViewHook? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return adWebViewHook?.onTouchEvent?.let { it(event) } ?: super.onTouchEvent(event)
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode {
        return super.startActionMode(callback)
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode {
        val webView = this
        val myCallback = object : ActionMode.Callback2() {
            var customMenu: CustomMenu? = null
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

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val res = this.customMenu?.let {
                    // @TODO 这里通过SystemUIFFI来实现自定义菜单
                    for (m in it.menus) {
                        menu.add(m.key).setOnMenuItemClickListener {
                            webView.evaluateJavascript(m.value) {
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

            override fun onDestroyActionMode(mode: ActionMode) {
                this.customMenu = null
                println("2==============onDestroyActionMode===" + mode.title)
                callback?.onDestroyActionMode(mode)
            }

            override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
                try {
                    (callback as ActionMode.Callback2).onGetContentRect(mode, view, outRect)
                } catch (e: Exception) {
                    super.onGetContentRect(mode, view, outRect)
                }
            }

        }
        return super.startActionMode(myCallback, type)
    }

}


package org.dweb_browser.dwebview

import kotlin.js.Promise
import kotlin.math.*


//interface IDWebView {
//     fun loadUrl(url: String, force: Boolean = false): Promise<Unit>
//     fun getUrl(): String
//     fun getTitle(): String
//     fun getIcon(): Promise<String>
//     fun destroy()
//     fun canGoBack(): Boolean
//     fun canGoForward(): Boolean
//     fun goBack()
//     fun goForward()
//
//    /**
//     * 在 electron 的范畴内，主进程同 渲染进程之间通信是不需要创建
//     * messageChannel 的所以
//     */
////     fun createMessageChannel(): IMessageChannel
//
//    /**
//     * 向webview发送消息
//     * TODO("发送消息给webview中载入的htm内容")
//     */
//    fun send(channel: String, message: String)
//
//    /**
//     * 添加监听器
//     * TODO("监听通过某个频道中从webView的内容中发送的消息")
//     */
//    fun on(channel: String, listener: ipcMainListener<Any>){
//
//    }
//
//    /**
//     * TODO("只监听一次webview通过某个频道发送过来的消息")
//     */
//    fun once(channel: String, listener: ipcMainListener<Any>)
//
//    /**
//     * TODO("移除某一个频道的监听器")
//     */
//    fun removeListener(channel: String, listener: ipcMainRemoveListener<Any>)
//
//    /**
//     * TODO("移除某个频道上的全部监听器")
//     */
//    fun removeAllListeners(channel: String)
//
//    /**
//     * 缩、放 webveiw 的尺寸
//     * TODO("缩放 webview 的尺寸")
//     */
//     fun setContentScale(scale: Float)
//
//    /**
//     * 执行一段JS代码，这个代码将会包裹在 (async()=>{ YOUR_CODE })() 中
//     * 返回一个字符串？？
//     * TODO("让 webveiw 执行指定代码的方法")
//     */
//    fun evalAsyncJavascript(code: String, userGesture: Boolean): Promise<Any>
//}


class DWebView(
    private val engine: DWebViewEngine
):  IDWebView{

    /**
     * BrowserView 被添加到的 window 对象
     */
    private var _win: Electron.BrowserWindow? = null

    /**
     * 从 BrowserWindow 上删除当前的 BroserView
     */
    fun detachFromWindow(){
        _win?.removeBrowserView(engine)
    }

    /**
     * 把 webView 添加到 window 上
     */
    fun attachToWindow(win: Electron.BrowserWindow){
        _win = win
        win.addBrowserView(engine)
    }

    /**
     * webview 载入一个 url
     */
    override fun loadUrl(url: String, force: Boolean): Promise<Unit> {
        return engine.webContents.loadURL(url)
    }

    /**
     * 获取 webview 载入的 url 地址
     */
    override fun getUrl(): String  = engine.webContents.getURL()

    override fun getTitle(): String = engine.webContents.getTitle()

    override fun getIcon(): Promise<String> {
        // 返回一个 Promise
        return Promise<String>{
            resolve, reject ->
            // 添加一个平台监听器
            engine.webContents.ipc.once("get-icon"){
                _: Electron.IpcMainEvent, arg: String -> resolve(arg as String)
            }
            // 执行查询时间
            engine.webContents.executeJavascript("""
            const links = document.querySelectorAll('link[rel~="icon"]')
            const iconUrl = links[0].href
            // 下面的代码必须要有匹配 preload 
            window.electron.messageSend("get-icon", iconUrl)
        """.trimIndent(), true)
        }
    }

    override fun destroy()  = engine.webContents.close()

    override fun canGoBack(): Boolean  = engine.webContents.canGoBack()

    override fun canGoForward(): Boolean  = engine.webContents.canGoForward()

    override fun goBack() = engine.webContents.goBack()

    override fun goForward() = engine.webContents.goForward()

    override fun send(channel: String, message: String)  = engine.webContents.send(channel, message)

    override fun on(channel: String, listener: ipcMainListener<Any>) {
        engine.webContents.ipc.on(channel, listener)
    }

    override fun once(channel: String, listener: ipcMainListener<Any>) {
        engine.webContents.ipc.once(channel, listener)
    }

    override fun removeListener(channel: String, listener: ipcMainRemoveListener<Any>) {
        engine.webContents.ipc.removeListener(channel, listener)
    }

    override fun removeAllListeners(channel: String) {
        engine.webContents.ipc.removeAllListeners(channel)
    }

    override fun setContentScale(scale: Float): Unit {
        // 获取当前尺寸
        // 计算赋值
        // 以当前左上角坐标不变为基础的缩放
        val rect = engine.getBounds()
        engine.setBounds(object: Electron.Rectangle{
            override val x: Int = rect.x
            override val y: Int = rect.y
            override val width: Int = (rect.width * scale).toInt()
            override val height: Int = (rect.height * scale).toInt()
        })
    }

    override fun evalAsyncJavascript(code: String, userGesture: Boolean): Promise<Any> {
        return engine.webContents.executeJavascript(code, userGesture)
    }
}

package org.dweb_browser.dwebview

import kotlin.js.Promise
import kotlin.math.*

class DWebView(
  private val engine: DWebViewEngine
) : IDWebView {

  /**
   * BrowserView 被添加到的 window 对象
   */
  private var _win: Electron.BrowserWindow? = null

  /**
   * 从 BrowserWindow 上删除当前的 BroserView
   */
  fun detachFromWindow() {
    _win?.removeBrowserView(engine)
  }

  /**
   * 把 webView 添加到 window 上
   */
  fun attachToWindow(win: Electron.BrowserWindow) {
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
  override fun getUrl(): String = engine.webContents.getURL()

  override fun getTitle(): String = engine.webContents.getTitle()

  override fun getIcon(): Promise<String> {
    // 返回一个 Promise
    return Promise<String> { resolve, reject ->
      // 添加一个平台监听器
      engine.webContents.ipc.once("get-icon") { _: Electron.IpcMainEvent, arg: String ->
        resolve(arg as String)
      }
      // 执行查询时间
      engine.webContents.executeJavascript(
        """
            const links = document.querySelectorAll('link[rel~="icon"]')
            const iconUrl = links[0].href
            // 下面的代码必须要有匹配 preload 
            window.electron.messageSend("get-icon", iconUrl)
        """.trimIndent(), true
      )
    }
  }

  override fun destroy() = engine.webContents.close()

  override fun canGoBack(): Boolean = engine.webContents.canGoBack()

  override fun canGoForward(): Boolean = engine.webContents.canGoForward()

  override fun goBack() = engine.webContents.goBack()

  override fun goForward() = engine.webContents.goForward()

  override fun send(channel: String, message: String) = engine.webContents.send(channel, message)

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

  override suspend fun setContentScale(
    scale: Float,
    width: Float,
    height: Float,
    density: Float,
  ): Unit {
    // 获取当前尺寸
    // 计算赋值
    // 以当前左上角坐标不变为基础的缩放
    engine.setBounds(object : Electron.Rectangle {
      override val x: Int = rect.x
      override val y: Int = rect.y
      override val width: Int = (width * scale).toInt()
      override val height: Int = (height * scale).toInt()
    })
  }

  override fun evalAsyncJavascript(code: String, userGesture: Boolean): Promise<Any> {
    return engine.webContents.executeJavascript(code, userGesture)
  }
}

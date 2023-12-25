package org.dweb_browser.dwebview

import kotlin.js.Promise


/**
 * npm electron 模块
 */


/**
 * 通过类型别名实现 multiplatform 的同一
 */
typealias DWebViewEngine = Electron.BrowserView


@JsModule("electron")
external object Electron {

  class BrowserWindow {
    fun addBrowserView(borserView: BrowserView): Unit
    fun removeBrowserView(browserView: BrowserView): Unit
  }


  interface WebContents {
    fun loadURL(url: String): Promise<Unit>
    fun openDevTools(): Unit

    fun executeJavascript(code: String, userGesture: Boolean): Promise<Any>
    fun getURL(): String
    fun getTitle(): String

    fun close(): Unit
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean

    fun goBack(): Unit

    fun goForward(): Unit

    fun send(channel: String, Message: String): Unit
    val ipc: IpcMain
  }

  class BrowserView {
    val webContents: WebContents
    fun setBounds(options: Rectangle): Unit

    fun getBounds(): Rectangle
  }

  interface Rectangle {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
  }

  /**
   * ipc 通信 主进程端的接口
   *
   */
  interface IpcMain {
    fun <T> on(channel: String, listener: ipcMainListener<T>): Unit
    fun <T> once(channel: String, listener: ipcMainListener<T>): Unit

    fun removeListener(channel: String, listener: ipcMainRemoveListener<Any>)
    fun removeAllListeners(channel: String)
  }

  /**
   * Electron 主进程 Ipc 通信的事件的接口
   */
  interface IpcMainEvent : Event {
    val processId: Int
    val frameId: Int

    //        val returnValue: Any
    val sender: WebContents

    // 暂时不用
    // val senderFrame: WebFramework
    // 暂时不用
    // val ports: Array<MessagePortMain>
    fun reply(channel: String, args: Array<Any>): Unit
  }

  /**
   * Electron.Event接口
   */
  interface Event : GlobalEvent {
    fun preventDefault(): Unit
  }

}


/**
 * 声明一个 Electron.ipcMain 添加 频道监听器的
 * 监听器函数类型
 */
typealias ipcMainListener<T> = (event: Electron.IpcMainEvent, args: T) -> Unit

/**
 * 声明一个 Electron.ipcMain 删除 频道监听器的
 * 监听器函数类型
 */
typealias ipcMainRemoveListener<T> = (arg: T) -> Unit

/**
 * 申明一个全局的GlobalEvent类型
 * 这个类型继承原 dom-event.d.ts 文件中的
 * global.Event
 */
external interface GlobalEvent : Event {
  val returnValue: Any
}


/**
 * 只支持 nodejs 的全局 Event 接口类型
 */
external interface Event {

}





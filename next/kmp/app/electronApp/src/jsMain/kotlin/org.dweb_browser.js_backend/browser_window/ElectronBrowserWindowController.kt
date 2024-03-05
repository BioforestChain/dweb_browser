package org.dweb_browser.js_backend.browser_window

import electron.BrowserWindow
import electron.BrowserWindowConstructorOptions
import electron.Rectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.js_backend.http.SubDomainHttpServer
import electron.app
import electron.core.BrowserWindowEvent
import electron.core.Event
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import node.http.IncomingMessage
import node.http.ServerResponse
import org.dweb_browser.js_backend.http.MatchPattern
import org.dweb_browser.js_backend.http.Method
import node.url.parse

typealias OnBrowserTitleUpdateCallback = (event: Event, title: String, explicitSet: Boolean) -> Unit
typealias OnCloseCallback = (event: Event) -> Unit
typealias OnClosedCallback = () -> Unit
typealias OnSessionEndCallback = () -> Unit
typealias OnUnresponsiveCallback = () -> Unit
typealias OnResponsiveCallback = () -> Unit
typealias OnBlurCallback = () -> Unit
typealias OnFocusCallback = () -> Unit
typealias OnShowCallback = () -> Unit
typealias OnHideCallback = () -> Unit
typealias OnReadyToShowCallback = () -> Unit
typealias OnMaximizeCallback = () -> Unit
typealias OnUnmaximizeCallback = () -> Unit
typealias OnRestoreCallback = () -> Unit
typealias OnWillResizeCallback = (event: Event, newBounds: Rectangle, details: dynamic) -> Unit
typealias OnResizeCallback = () -> Unit
typealias OnResizedCallback = () -> Unit
typealias OnWillMoveCallback = (event: Event, newBounds: Rectangle) -> Unit
typealias OnMoveCallback = () -> Unit
typealias OnMovedCallback = () -> Unit
typealias OnEnterFullScreenCallback = () -> Unit
typealias OnLeaveFullScreenCallback = () -> Unit
typealias OnEnterHtmlFullScreenCallback = () -> Unit
typealias OnLeaveHtmlFullScreenCallback = () -> Unit
typealias OnAlwaysOnTopChangedCallback = (event: Event, isAlwaysOnTop: Boolean) -> Unit
typealias OnAppCommandCallback = (event: Event, command: String) -> Unit

/**
 * 用来控制ElectronBrowserWindow的Controller
 *
 * 1.功能目标
 * - 接受Client发送过来的控制指令???
 * - 完成对BrowserWindow的相应操作
 *
 * 2.设计概述
 * - 通过 httpServer 接受Client 指令
 * - 指令规范 /browser_window_controller?command=xxx&params=xxx
 * - 如果没有注册的指令返回404 command not fount
 * - 如果有注册的指令返回操作后的结果
 * - subDomain作为标识符
 *
 * 3.依赖
 * - SubDomainHttpServer
 */
class ElectronBrowserWindowController private constructor(
    val subDomain: String
){
    private val scop = CoroutineScope(Dispatchers.Default)
    private val subDomainHttpServer = SubDomainHttpServer(subDomain)
    val baseBrowserWindowModelReady = CompletableDeferred<ElectronBrowserWindowController>()
    private lateinit var browserWindow: BrowserWindow
    fun open(electronBrowserWindowOptions: BrowserWindowConstructorOptions): CompletableDeferred<ElectronBrowserWindowController>{
        scop.launch {
            app.whenReady().await()
            browserWindow = BrowserWindow(electronBrowserWindowOptions).apply {
                initBrowserWindowAddEventListener()
            }
            baseBrowserWindowModelReady.complete(this@ElectronBrowserWindowController)
            browserWindow.loadURL("${subDomainHttpServer.getBaseUrl()}/index.html")
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                browserWindow.webContents.openDevTools()
            }
            console.log(browserWindow.isMovable())
        }
        return baseBrowserWindowModelReady
    }

    private val _onPageTitleUpdatedList = mutableListOf<OnBrowserTitleUpdateCallback>()
    private val _onCloseList = mutableListOf<OnCloseCallback>()
    private val _onClosedList = mutableListOf<OnClosedCallback>()
    private val _onSessionEndList = mutableListOf<OnSessionEndCallback>()
    private val _onUnresponsiveList = mutableListOf<OnUnresponsiveCallback>()
    private val _onResponsiveList = mutableListOf<OnResponsiveCallback>()
    private val _onBlurList = mutableListOf<OnBlurCallback>()
    private val _onFocusList = mutableListOf<OnFocusCallback>()
    private val _onShowList = mutableListOf<OnShowCallback>()
    private val _onHideList = mutableListOf<OnHideCallback>()
    private val _onReadyToShowList = mutableListOf<OnReadyToShowCallback>()
    private val _onMaximizeList = mutableListOf<OnMaximizeCallback>()
    private val _onUnmaximizeList = mutableListOf<OnUnmaximizeCallback>()
    private val _onRestoreList = mutableListOf<OnRestoreCallback>()
    private val _onWillResizeList = mutableListOf<OnWillResizeCallback>()
    private val _onResizeList = mutableListOf<OnResizeCallback>()
    private val _onResizedList = mutableListOf<OnResizedCallback>()
    private val _onWillMoveList = mutableListOf<OnWillMoveCallback>()
    private val _onMoveList = mutableListOf<OnMoveCallback>()
    private val _onMovedList = mutableListOf<OnMovedCallback>()
    private val _onEnterFullScreenList = mutableListOf<OnEnterFullScreenCallback>()
    private val _onLeaveFullScreenList = mutableListOf<OnLeaveFullScreenCallback>()
    private val _onEnterHtmlFullScreenList = mutableListOf<OnEnterHtmlFullScreenCallback>()
    private var _onLeaveHtmlFullScreenList = mutableListOf<OnLeaveHtmlFullScreenCallback>()
    private val _onAlwaysOnTopChangedList = mutableListOf<OnAlwaysOnTopChangedCallback>()
    private val _onAppCommandList = mutableListOf<OnAppCommandCallback>()

    private fun BrowserWindow.initBrowserWindowAddEventListener(){
        on(BrowserWindowEvent.PAGE_TITLE_UPDATED){ event, title, explicitSet ->
            _onPageTitleUpdatedList.forEach { cb -> cb(event, title, explicitSet) }
        }
        on(BrowserWindowEvent.CLOSE){ event ->
            _onCloseList.forEach { cb -> cb(event) }
        }
        on(BrowserWindowEvent.CLOSED){
            _onClosedList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.SESSION_END){
            _onSessionEndList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.UNRESPONSIVE){
            _onUnresponsiveList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.RESPONSIVE){
            _onResponsiveList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.BLUR){
            _onBlurList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.FOCUS){
            _onFocusList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.SHOW){
            _onShowList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.HIDE){
            _onHideList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.READY_TO_SHOW){
            _onReadyToShowList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.MAXIMIZE){
            _onMaximizeList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.UNMAXIMIZE){
            _onUnmaximizeList.forEach{ cb -> cb()}
        }
        on(BrowserWindowEvent.RESTORE){
            _onRestoreList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.WILL_RESIZE){ event, newBounds, details->
            _onWillResizeList.forEach { cb -> cb(event, newBounds, details) }
        }
        on(BrowserWindowEvent.RESIZE){
            _onResizeList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.RESIZED){
            _onResizedList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.WILL_MOVE){ event, newBounds ->
            _onWillMoveList.forEach { cb -> cb(event, newBounds) }
        }
        on(BrowserWindowEvent.MOVE){
            _onMoveList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.MOVED){
            _onMovedList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.ENTER_FULL_SCREEN){
            _onEnterFullScreenList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.LEAVE_FULL_SCREEN){
            _onLeaveFullScreenList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.ENTER_HTML_FULL_SCREEN){
            _onEnterHtmlFullScreenList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.LEAVE_HTML_FULL_SCREEN){
            _onLeaveHtmlFullScreenList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.ALWAYS_ON_TOP_CHANGED){ event, isAlwaysOnTop->
            _onAlwaysOnTopChangedList.forEach { cb -> cb(event, isAlwaysOnTop) }
        }
        on(BrowserWindowEvent.APP_COMMAND){ event, command ->
            _onAppCommandList.forEach { cb -> cb(event, command) }
        }
    }

    fun onPageTitle(cb: OnBrowserTitleUpdateCallback): () -> Unit {
        _onPageTitleUpdatedList.add(cb)
        return { _onPageTitleUpdatedList.remove(cb) }
    }

    fun onClose(cb: OnCloseCallback): () -> Unit {
        _onCloseList.add(cb)
        return { _onCloseList.remove(cb) }
    }

    fun onClosed(cb: OnClosedCallback): () -> Unit {
        _onClosedList.add(cb)
        return { _onClosedList.remove(cb) }
    }

    fun onSessionEnd(cb: OnSessionEndCallback): () -> Unit {
        _onSessionEndList.add(cb)
        return { _onSessionEndList.remove(cb) }
    }

    fun onUnresponsive(cb: OnUnresponsiveCallback): () -> Unit {
        _onUnresponsiveList.add(cb)
        return { _onUnresponsiveList.remove(cb) }
    }

    fun onResponsive(cb: OnResponsiveCallback): () -> Unit {
        _onResponsiveList.add(cb)
        return { _onResponsiveList.remove(cb) }
    }

    fun onBlur(cb: OnBlurCallback): () -> Unit {
        _onBlurList.add(cb)
        return { _onBlurList.remove(cb) }
    }

    fun onFocus(cb: OnFocusCallback): () -> Unit {
        _onFocusList.add(cb)
        return { _onFocusList.remove(cb) }
    }

    fun onShow(cb: OnShowCallback): () -> Unit {
        _onShowList.add(cb)
        return { _onShowList.remove(cb) }
    }

    fun onHide(cb: OnHideCallback): () -> Unit {
        _onHideList.add(cb)
        return { _onHideList.remove(cb) }
    }

    fun onReadyToShow(cb: OnReadyToShowCallback): () -> Unit {
        _onReadyToShowList.add(cb)
        return { _onReadyToShowList.remove(cb) }
    }

    fun onMaximize(cb: OnMaximizeCallback): () -> Unit {
        _onMaximizeList.add(cb)
        return { _onMaximizeList.remove(cb) }
    }

    fun onUnmaximize(cb: OnUnmaximizeCallback): () -> Unit {
        _onUnmaximizeList.add(cb)
        return { _onUnmaximizeList.remove(cb) }
    }

    fun onRestore(cb: OnRestoreCallback): () -> Unit {
        _onRestoreList.add(cb)
        return { _onRestoreList.remove(cb) }
    }

    fun onWillResize(cb: OnWillResizeCallback): () -> Unit {
        _onWillResizeList.add(cb)
        return { _onWillResizeList.remove(cb) }
    }

    fun onResize(cb: OnResizeCallback): () -> Unit {
        _onResizeList.add(cb)
        return { _onResizeList.remove(cb) }
    }

    fun onResized(cb: OnResizedCallback): () -> Unit {
        _onResizedList.add(cb)
        return { _onResizedList.remove(cb) }
    }

    fun onWillMove(cb: OnWillMoveCallback): () -> Unit {
        _onWillMoveList.add(cb)
        return { _onWillMoveList.remove(cb) }
    }

    fun onMove(cb: OnMoveCallback): () -> Unit {
        _onMoveList.add(cb)
        return { _onMoveList.remove(cb) }
    }

    fun onMoved(cb: OnMovedCallback): () -> Unit {
        _onMovedList.add(cb)
        return { _onMovedList.remove(cb) }
    }

    fun onEnterFullScreen(cb: OnEnterFullScreenCallback): () -> Unit {
        _onEnterFullScreenList.add(cb)
        return { _onEnterFullScreenList.remove(cb) }
    }

    fun onLeaveFullScreen(cb: OnLeaveFullScreenCallback): () -> Unit {
        _onLeaveFullScreenList.add(cb)
        return { _onLeaveFullScreenList.remove(cb) }
    }

    fun onEnterHtmlFullScreen(cb: OnEnterHtmlFullScreenCallback): () -> Unit {
        _onEnterHtmlFullScreenList.add(cb)
        return { _onEnterHtmlFullScreenList.remove(cb) }
    }

    fun onLeaveHtmlFullScreen(cb: OnLeaveHtmlFullScreenCallback): () -> Unit {
        _onLeaveHtmlFullScreenList.add(cb)
        return { _onLeaveHtmlFullScreenList.remove(cb) }
    }

    fun onAlwaysOnTopChanged(cb: OnAlwaysOnTopChangedCallback): () -> Unit {
        _onAlwaysOnTopChangedList.add(cb)
        return { _onAlwaysOnTopChangedList.remove(cb) }
    }

    fun onAppCommand(cb: OnAppCommandCallback): () -> Unit {
        _onAppCommandList.add(cb)
        return { _onAppCommandList.remove(cb) }
    }

    fun devToolsOpen(){
        browserWindow.webContents.openDevTools()
    }

    fun devToolsClose(){
        browserWindow.webContents.closeDevTools()
    }

    init {
        scop.launch{
            subDomainHttpServer.addRoute("/browser-window-operation", Method.GET, MatchPattern.FULL){req: IncomingMessage, res: ServerResponse<*> ->
                val query: dynamic = req.url?.let { parse(it, true).query }
                when(query["operation"]){
                    "close" -> browserWindow.close()
                    "reload" -> browserWindow.reload()
                }
            }
            subDomainHttpServer.whenReady.await().start()
            console.log("subDomainHttpServer run at : ${subDomainHttpServer.getBaseUrl()}")
        }
        allElectronBrowserWindowController[subDomain] = this
    }


    companion object {
        val allElectronBrowserWindowController = AllElectronBrowserWindowController()
        fun create(subDomain: String): ElectronBrowserWindowController{
            return ElectronBrowserWindowController(subDomain)
        }


        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        fun createBrowserWindowOptions(): BrowserWindowConstructorOptions{
            return {} as BrowserWindowConstructorOptions
        }
    }
}




class AllElectronBrowserWindowController(){
    private val mutableMap = mutableMapOf<String, ElectronBrowserWindowController>()
    operator fun set(key: String, value: ElectronBrowserWindowController){
        if(mutableMap[key] != null){
            console.error("""
                AllElectronBrowserWindowController.set 已经添加过一次了
                at fun set(key: String, value: ElectronBrowserWindowController)
                at AllElectronBrowserWindowController
            """.trimIndent())
        }
        mutableMap.put(key, value)
    }

    operator fun get(key: String): ElectronBrowserWindowController?{
        return mutableMap.get(key)
    }
}


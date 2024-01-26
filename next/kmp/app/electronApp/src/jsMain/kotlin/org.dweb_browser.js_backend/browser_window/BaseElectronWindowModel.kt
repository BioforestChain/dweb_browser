package org.dweb_browser.js_backend.browser_window

import electron.BrowserWindow
import info.bagen.dwebbrowser.ElectronAppHttpServer
import kotlinx.coroutines.launch
import org.dweb_browser.js_backend.http.HttpServer
import electron.app
import electron.BrowserWindowConstructorOptions
import electron.Rectangle
import electron.core.BrowserWindowConstructorOptionsBackgroundMaterial
import electron.core.BrowserWindowConstructorOptionsTitleBarStyle
import electron.core.BrowserWindowConstructorOptionsVibrancy
import electron.core.BrowserWindowConstructorOptionsVisualEffectState
import electron.core.Event
import electron.core.Point
import electron.core.WebPreferences
import info.bagen.dwebbrowser.await
import org.dweb_browser.js_backend.view_model.BaseViewModel
import electron.core.BrowserWindowEvent
import kotlinx.coroutines.CompletableDeferred
import node.http.IncomingMessage
import node.http.ServerResponse
import node.url.parse
import node.querystring.parse as queryStringParse
import org.dweb_browser.js_backend.http.MatchPattern
import org.dweb_browser.js_backend.http.Method
import org.dweb_browser.js_backend.http.Route



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
 * View ===  Window
 */
abstract class BaseBrowserWindowModel(frontendViewModelId: String) :
    BaseViewModel(frontendViewModelId) {
    lateinit var electronAppHttpSever: HttpServer
    lateinit var browserWindow: BrowserWindow
    val baseBrowserWindowModelReady = CompletableDeferred<BaseBrowserWindowModel>()
    abstract val electronBrowserWindowOptions: BrowserWindowConstructorOptions
    abstract val electronLoadUrlPath: String;
    abstract val electronIsOpenDevtools: Boolean;

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


    init {
        scope.launch {
            electronAppHttpSever = HttpServer.createHttpServer().await()
            electronAppHttpSever.routeAdd(
                // 用来处理从前端发起的操作window的请求
                // 格式：xxx/browser-window-operation?operation=close&frontendViewModelId=xxx
                // operation=close 定义操作
                // frontendViewModelId=xxx前后端匹配的viewModelId
                Route("/browser-window-operation", Method.GET, MatchPattern.PREFIX, ::_electronAppHttpSeverHandler),
            )
            console.log("没有打开BrowserWindow??")
//            需要删除 - 需要恢复打开electronApp
//            app.whenReady().await()
//            browserWindow = BrowserWindow(electronBrowserWindowOptions).apply {
//                initBrowserWindowAddEventListener()
//            }
//            baseBrowserWindowModelReady.complete(this@BaseBrowserWindowModel)
//            browserWindow.loadURL("${electronAppHttpSever.getAddress()}${electronLoadUrlPath}")
//            if (electronIsOpenDevtools) browserWindow.webContents.openDevTools()
        }

    }

    private fun _electronAppHttpSeverHandler(req: IncomingMessage, res: ServerResponse<*>){
        (req.url?.let { parse(it, true).query }?:throw(Throwable("""
            req.url === null
            req.url: ${req.url}
            at _electronAppHttpSeverHandler
            at BaseElectronWindowModel.kt
        """.trimIndent()))).let {it: dynamic->
            if(it["frontendViewModelId"] == frontendViewModelId){
                when(it["operation"]){
                    BrowserWindowEvent.CLOSE -> browserWindow.close()
                }
            }
        }
    }


    private  interface OperationAction{
        val operation: String
        val frontendViewModelId: String
    }

    private fun BrowserWindow.initBrowserWindowAddEventListener(){
        on(BrowserWindowEvent.PAGE_TITLE_UPDATED){event, title, explicitSet ->
            _onPageTitleUpdatedList.forEach { cb -> cb(event, title, explicitSet) }
        }
        on(BrowserWindowEvent.CLOSE){event ->
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
        on(BrowserWindowEvent.WILL_RESIZE){event, newBounds, details->
            _onWillResizeList.forEach { cb -> cb(event, newBounds, details) }
        }
        on(BrowserWindowEvent.RESIZE){
            _onResizeList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.RESIZED){
            _onResizedList.forEach { cb -> cb() }
        }
        on(BrowserWindowEvent.WILL_MOVE){event, newBounds ->
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
        on(BrowserWindowEvent.ALWAYS_ON_TOP_CHANGED){event, isAlwaysOnTop->
            _onAlwaysOnTopChangedList.forEach { cb -> cb(event, isAlwaysOnTop) }
        }
        on(BrowserWindowEvent.APP_COMMAND){event, command ->
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
        if (electronIsOpenDevtools) browserWindow.webContents.openDevTools()
    }

    fun devToolsClose(){
        browserWindow.webContents.closeDevTools()
    }

}


class ElectronBrowserWindowOptions(
    @JsName("width") val width: Double? = null,
    @JsName("height") val height: Double? = null,
    @JsName("x") var x: Double? = null,
    @JsName("y") var y: Double? = null,
    @JsName("useContentSize") var useContentSize: Boolean? = null,
    @JsName("center") var center: Boolean? = null,
    @JsName("minWidth") var minWidth: Double? = null,
    @JsName("minHeight") var minHeight: Double? = null,
    @JsName("maxWidth") var maxWidth: Double? = null,
    @JsName("maxHeight") var maxHeight: Double? = null,
    @JsName("resizable") var resizable: Boolean? = null,
    @JsName("movable") var movable: Boolean? = null,
    @JsName("minimizable") var minimizable: Boolean? = null,
    @JsName("maximizable") var maximizable: Boolean? = null,
    @JsName("closable") var closable: Boolean? = true,
    @JsName("focusable") var focusable: Boolean? = null,
    @JsName("alwaysOnTop") var alwaysOnTop: Boolean? = null,
    @JsName("fullscreen") var fullscreen: Boolean? = null,
    @JsName("fullscreenable") var fullscreenable: Boolean? = null,
    @JsName("simpleFullscreen") var simpleFullscreen: Boolean? = null,
    @JsName("skipTaskbar") var skipTaskbar: Boolean? = null,
    @JsName("hiddenInMissionControl") var hiddenInMissionControl: Boolean? = null,
    @JsName("kiosk") var kiosk: Boolean? = null,
    @JsName("title") var title: String? = null,
    @JsName("icon") var icon: (Any /* (NativeImage) | (string) */)? = null,
    @JsName("show") var show: Boolean? = true,
    @JsName("paintWhenInitiallyHidden") var paintWhenInitiallyHidden: Boolean? = null,
    @JsName("frame") var frame: Boolean? = true,
    @JsName("parent") var parent: electron.core.BrowserWindow? = null,
    @JsName("modal") var modal: Boolean? = false,
    @JsName("acceptFirstMouse") var acceptFirstMouse: Boolean? = null,
    @JsName("disableAutoHideCursor") var disableAutoHideCursor: Boolean? = null,
    @JsName("autoHideMenuBar") var autoHideMenuBar: Boolean? = null,
    @JsName("enableLargerThanScreen") var enableLargerThanScreen: Boolean? = null,
    @JsName("backgroundColor") var backgroundColor: String? = null,
    @JsName("hasShadow") var hasShadow: Boolean? = null,
    @JsName("opacity") var opacity: Double? = null,
    @JsName("darkTheme") var darkTheme: Boolean? = null,
    @JsName("transparent") var transparent: Boolean? = null,
    @JsName("type") var type: String? = null,
    @JsName("visualEffectState") var visualEffectState: (BrowserWindowConstructorOptionsVisualEffectState)? = null,
    @JsName("titleBarStyle") var titleBarStyle: (BrowserWindowConstructorOptionsTitleBarStyle)? = null,
    @JsName("trafficLightPosition") var trafficLightPosition: Point? = null,
    @JsName("roundedCorners") var roundedCorners: Boolean? = null,
    @JsName("thickFrame") var thickFrame: Boolean? = null,
    @JsName("vibrancy") var vibrancy: (BrowserWindowConstructorOptionsVibrancy)? = null,
    @JsName("backgroundMaterial") var backgroundMaterial: (BrowserWindowConstructorOptionsBackgroundMaterial)? = null,
    @JsName("zoomToPageWidth") var zoomToPageWidth: Boolean? = null,
    @JsName("tabbingIdentifier") var tabbingIdentifier: String? = null,
    @JsName("webPreferences") var webPreferences: WebPreferences? = null,
    @JsName("titleBarOverlay") var titleBarOverlay: (Any /* (TitleBarOverlay) | (boolean) */)? = null
) {
    companion object {
        fun create(): BrowserWindowConstructorOptions {
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") return ElectronBrowserWindowOptions(
                width = 1000.0, height = 1000.0
            ) as BrowserWindowConstructorOptions
        }
    }
}


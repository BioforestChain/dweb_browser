//package org.dweb_browser.js_backend.browser_window
//import electron.BrowserWindowConstructorOptions
//import electron.core.BrowserWindowConstructorOptionsBackgroundMaterial
//import electron.core.BrowserWindowConstructorOptionsTitleBarStyle
//import electron.core.BrowserWindowConstructorOptionsVibrancy
//import electron.core.BrowserWindowConstructorOptionsVisualEffectState
//import electron.core.Point
//import electron.core.WebPreferences
//
//class ElectronBrowserWindowOptions(
////    @JsName("width") val width: Double? = null,
////    @JsName("height") val height: Double? = null,
////    @JsName("x") var x: Double? = null,
////    @JsName("y") var y: Double? = null,
////    @JsName("useContentSize") var useContentSize: Boolean? = null,
////    @JsName("center") var center: Boolean? = null,
////    @JsName("minWidth") var minWidth: Double? = null,
////    @JsName("minHeight") var minHeight: Double? = 0.0,
////    @JsName("maxWidth") var maxWidth: Double? = null,
////    @JsName("maxHeight") var maxHeight: Double? = null,
////    @JsName("resizable") var resizable: Boolean? = true, // 可以拖动改变尺寸
////    @JsName("movable") var movable: Boolean? = true, // 默认可以拖动
////    @JsName("minimizable") var minimizable: Boolean? = null,
////    @JsName("maximizable") var maximizable: Boolean? = null,
////    @JsName("closable") var closable: Boolean? = true,
////    @JsName("focusable") var focusable: Boolean? = null,
////    @JsName("alwaysOnTop") var alwaysOnTop: Boolean? = null,
////    @JsName("fullscreen") var fullscreen: Boolean? = null,
////    @JsName("fullscreenable") var fullscreenable: Boolean? = null,
////    @JsName("simpleFullscreen") var simpleFullscreen: Boolean? = null,
////    @JsName("skipTaskbar") var skipTaskbar: Boolean? = null,
////    @JsName("hiddenInMissionControl") var hiddenInMissionControl: Boolean? = null,
////    @JsName("kiosk") var kiosk: Boolean? = null,
////    @JsName("title") var title: String? = null,
////    @JsName("icon") var icon: (Any /* (NativeImage) | (string) */)? = null,
////    @JsName("show") var show: Boolean? = true,
////    @JsName("paintWhenInitiallyHidden") var paintWhenInitiallyHidden: Boolean? = null,
////    @JsName("frame") var frame: Boolean? = true,
////    @JsName("parent") var parent: electron.core.BrowserWindow? = null,
////    @JsName("modal") var modal: Boolean? = false,
////    @JsName("acceptFirstMouse") var acceptFirstMouse: Boolean? = null,
////    @JsName("disableAutoHideCursor") var disableAutoHideCursor: Boolean? = null,
////    @JsName("autoHideMenuBar") var autoHideMenuBar: Boolean? = null,
////    @JsName("enableLargerThanScreen") var enableLargerThanScreen: Boolean? = null,
////    @JsName("backgroundColor") var backgroundColor: String? = null,
////    @JsName("hasShadow") var hasShadow: Boolean? = null,
////    @JsName("opacity") var opacity: Double? = null,
////    @JsName("darkTheme") var darkTheme: Boolean? = null,
////    @JsName("transparent") var transparent: Boolean? = null,
////    @JsName("type") var type: String? = null,
////    @JsName("visualEffectState") var visualEffectState: (BrowserWindowConstructorOptionsVisualEffectState)? = null,
////    @JsName("titleBarStyle") var titleBarStyle: (BrowserWindowConstructorOptionsTitleBarStyle)? = null,
////    @JsName("trafficLightPosition") var trafficLightPosition: Point? = null,
////    @JsName("roundedCorners") var roundedCorners: Boolean? = null,
////    @JsName("thickFrame") var thickFrame: Boolean? = null,
////    @JsName("vibrancy") var vibrancy: (BrowserWindowConstructorOptionsVibrancy)? = null,
////    @JsName("backgroundMaterial") var backgroundMaterial: (BrowserWindowConstructorOptionsBackgroundMaterial)? = null,
////    @JsName("zoomToPageWidth") var zoomToPageWidth: Boolean? = null,
////    @JsName("tabbingIdentifier") var tabbingIdentifier: String? = null,
////    @JsName("webPreferences") var webPreferences: WebPreferences? = null,
////    @JsName("titleBarOverlay") var titleBarOverlay: (Any /* (TitleBarOverlay) | (boolean) */)? = null
////) {
////    companion object {
////        fun create(): BrowserWindowConstructorOptions {
////            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") return ElectronBrowserWindowOptions(
//////                width = 1000.0, height = 1000.0
////            ) as BrowserWindowConstructorOptions
////        }
////    }
////}

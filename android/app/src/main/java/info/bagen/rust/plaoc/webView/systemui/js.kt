package info.bagen.rust.plaoc.webView.systemui


import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import info.bagen.rust.plaoc.webView.jsutil.BoolInt
import info.bagen.rust.plaoc.webView.jsutil.DataString_From
import info.bagen.rust.plaoc.webView.jsutil.JsUtil
import info.bagen.rust.plaoc.webView.jsutil.toBoolean
import info.bagen.rust.plaoc.webView.network.getColorHex
import info.bagen.rust.plaoc.webView.network.hexToIntColor
import info.bagen.rust.plaoc.webView.systemui.js.VirtualKeyboardFFI
import info.bagen.rust.plaoc.webkit.AdWebViewHook


private const val TAG = "SystemUiFFI"

class SystemUiFFI(
    private val activity: ComponentActivity,
    private val webView: WebView,
    private val hook: AdWebViewHook,
    private val jsUtil: JsUtil,
    private val systemUIState: SystemUIState,
) {
    //    @JavascriptInterface
    val virtualKeyboard =
        VirtualKeyboardFFI(systemUIState.virtualKeyboard.overlay, activity, webView)

    /**
     * @TODO 在未来，这里的disable与否，通过更加完善的声明来实现，比如可以声明多个rect
     */
    @JavascriptInterface
    fun disableTouchEvent() {
        hook.onTouchEvent = { false }
    }

    /**第一个参数是颜色HEX。第二个是图标是否更期望于使用深色*/
    fun setStatusBarBackgroundColor(colorHex: String): Boolean {
        systemUIState.statusBar.apply {
            color.value = Color(hexToIntColor(colorHex))
        }
        return true
    }

    fun setStatusBarStyle(darkIcons: Boolean): Boolean {
        systemUIState.statusBar.apply {
            isDarkIcons.value = darkIcons
        }
        return true
    }

    /** 获取状态栏背景颜色*/
    fun getStatusBarBackgroundColor(): String {
        val color = systemUIState.statusBar.color.value
        val colorInt = android.graphics.Color.argb(color.alpha, color.red, color.green, color.blue)
        return getColorHex(colorInt)
    }

    /** 获取状态栏是否更期望使用深色*/
    fun getStatusBarIsDark(): Boolean {
        return systemUIState.statusBar.isDarkIcons.value
            ?: (systemUIState.statusBar.color.value.luminance() > 0.5F)
    }

    /** 查看状态栏是否可见*/
    fun getStatusBarVisible(): Boolean {
        return systemUIState.statusBar.visible.value
    }

    /** 设置false为透明*/
    fun setStatusBarVisible(visible: String): Boolean {
        systemUIState.statusBar.visible.value = visible.toBoolean()
        return visible.toBoolean()
    }

    /**获取状态栏是否透明的状态*/
    fun getStatusBarOverlay(): Boolean {
        return systemUIState.statusBar.overlay.value
    }

    /**设置状态栏是否透明*/
    fun setStatusBarOverlay(isOverlay: String): Boolean {
        systemUIState.statusBar.overlay.value = isOverlay.toBoolean()
//        Log.i(TAG, "isOverlayStatusBar.value:${systemUIState.statusBar.overlay.value}")
        return true
    }

    /**设置系统导航栏颜色*/
    fun setNavigationBarColor(
        colorHex: String,
        darkIcons: Boolean,
        isNavigationBarContrastEnforced: Boolean
    ): Boolean {
        systemUIState.navigationBar.apply {
            color.value = Color(hexToIntColor(colorHex))
            isDarkIcons.value = darkIcons
            isContrastEnforced.value = isNavigationBarContrastEnforced
        }
        return true
    }

    /**获取系统导航栏可见性*/
    fun getNavigationBarVisible(): Boolean {
        return systemUIState.navigationBar.visible.value
    }

    /**设置系统导航栏是否隐藏*/
    fun setNavigationBarVisible(visible: String): Boolean {
        systemUIState.navigationBar.visible.value =
            visible.toBoolean()
        return true
    }

    /**获取系统导航栏是否透明*/
    fun getNavigationBarOverlay(): Boolean {
        return systemUIState.navigationBar.overlay.value
    }

    /**设置系统导航栏是否透明*/
    fun setNavigationBarOverlay(isOverlay: String): Boolean {
        systemUIState.navigationBar.overlay.value =
            isOverlay.toBoolean()
        return isOverlay.toBoolean()
    }


    private val insetsCompat: WindowInsetsCompat by lazy {
        WindowInsetsCompat.toWindowInsetsCompat(
            activity.window.decorView.rootWindowInsets
        )
    }

    private fun Insets.toJson(): String {
        return """{"top":${top},"left":${left},"bottom":${bottom},"right":${right}}"""
    }

    @JavascriptInterface
    fun getInsetsTypeEnum(): String {
        return DataString_From(InsetsType())
    }

    class InsetsType {
        val FIRST = 1
        val STATUS_BARS = FIRST
        val NAVIGATION_BARS = 1 shl 1
        val CAPTION_BAR = 1 shl 2

        val IME = 1 shl 3

        val SYSTEM_GESTURES = 1 shl 4
        val MANDATORY_SYSTEM_GESTURES = 1 shl 5
        val TAPPABLE_ELEMENT = 1 shl 6

        val DISPLAY_CUTOUT = 1 shl 7

        val LAST = 1 shl 8
        val SIZE = 9
        val WINDOW_DECOR = LAST
    }

    @JavascriptInterface
    fun getInsetsRect(typeMask: Int, ignoreVisibility: BoolInt): String {
        if (ignoreVisibility.toBoolean()) {
            return insetsCompat.getInsetsIgnoringVisibility(typeMask).toJson()
        }
        return insetsCompat.getInsets(typeMask).toJson()
    }

    @JavascriptInterface
    fun showInsets(typeMask: Int) {
        return WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .show(typeMask)
    }
}

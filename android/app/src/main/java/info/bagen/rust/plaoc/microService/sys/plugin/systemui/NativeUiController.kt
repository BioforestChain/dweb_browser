package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import java.lang.reflect.Type


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("nativeui", tag, msg, err)

class NativeUiController(
    val activity: ComponentActivity,
) {
    val windowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(
            activity.window, activity.window.decorView
        )
    }

    val statusBar = StatusBarController(activity, this)
    val navigationBar = NavigationBarController(activity, this)
    val virtualKeyboard = VirtualKeyboardController(activity, this)

    val safeArea = SafeAreaController(activity, this)


    @Composable
    fun effect(): NativeUiController {
        /**
         * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
         */
        SideEffect {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
            windowInsetsControllerCompat.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        statusBar.effect()
        navigationBar.effect()
        virtualKeyboard.effect()
        safeArea.effect()

        return this
    }

    @JsonAdapter(BarStyle::class)
    enum class BarStyle(val style: String) : JsonDeserializer<BarStyle>, JsonSerializer<BarStyle> {
        /**
         * Light text for dark backgrounds.
         */
        Dark("DARK"),

        /**
         * Dark text for light backgrounds.
         */
        Light("LIGHT"),

        /**
         * The style is based on the device appearance.
         * If the device is using Dark mode, the bar text will be light.
         * If the device is using Light mode, the bar text will be dark.
         * On Android the default will be the one the app was launched with.
         */
        Default("DEFAULT"), ;

        companion object {
            fun from(style: String): BarStyle {
                return values().find { it.style == style } ?: BarStyle.Default
            }
        }

        override fun deserialize(
            json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
        ): BarStyle = from(json.asString)

        override fun serialize(
            src: BarStyle, typeOfSrc: Type, context: JsonSerializationContext
        ): JsonElement = JsonPrimitive(src.style)

    }

    class StatusBarController(
        val activity: ComponentActivity,
        val nativeUiController: NativeUiController,
    ) {
        val overlayState = mutableStateOf(true)
        val colorState = mutableStateOf(Color.Transparent)
        val styleState = mutableStateOf(BarStyle.Default)
        val visibleState = mutableStateOf(true)
        val statusBarsInsetsState = mutableStateOf(WindowInsets(0))

        val observer = StateObservable { gson.toJson(toJsonAble()) }

        /**
         * 使得当前 StatusBarController 生效
         */
        @Composable
        fun effect(): StatusBarController {
            val systemUiController = rememberSystemUiController()

            statusBarsInsetsState.value = WindowInsets.statusBars

            val color by colorState
            val style by styleState
            var visible by visibleState
            DisposableEffect(visible, color, style) {
                debugNativeUi(
                    "DisposableEffect", "visible:${visible}"
                )
                systemUiController.isStatusBarVisible = visible
                systemUiController.setStatusBarColor(
                    color = color,
                    darkIcons = when (style) {
                        BarStyle.Dark -> true
                        BarStyle.Light -> false
                        else -> color.luminance() > 0.5F
                    },
                )
                onDispose {}
            }

            observer.stateChanges.also {
                it.rememberByState(overlayState)
                it.rememberByState(colorState)
                it.rememberByState(styleState)
                it.rememberByState(visibleState)
                it.rememberByState(statusBarsInsetsState)

                it.effectChange {
                    debugNativeUi("StatusBar", "CHANGED")
                    observer.notifyObserver()
                }
            }

            return this
        }


        data class StatusBarState(
            val visible: Boolean,
            val style: BarStyle,
            val overlay: Boolean,
            val color: ColorJson,
            val boundingRect: RectJson,
        )


        fun toJsonAble() = StatusBarState(
            visible = visibleState.value,
            style = styleState.value,
            overlay = overlayState.value,
            color = colorState.value.toJsonAble(),
            boundingRect = statusBarsInsetsState.value.toJsonAble()
        )
    }

    @Stable
    class NavigationBarController(
        val activity: ComponentActivity,
        val nativeUiController: NativeUiController,
    ) {
        /**
         * 是否层叠渲染
         */
        val overlayState = mutableStateOf(true)

        /**
         * 背景色
         */
        val colorState = mutableStateOf(Color.Transparent)

        /**
         * 前景风格
         */
        val styleState = mutableStateOf(BarStyle.Default)

        /**
         * 是否可见
         */
        val visibleState = mutableStateOf(true)

        val navigationBarsInsetsState = mutableStateOf(WindowInsets(0))

        val observer = StateObservable { gson.toJson(toJsonAble()) }

        @Composable
        fun effect(): NavigationBarController {
            val systemUiController = rememberSystemUiController()

            navigationBarsInsetsState.value = WindowInsets.navigationBars

            val color by colorState
            val style by styleState
            DisposableEffect(color, style) {
                systemUiController.setNavigationBarColor(
                    color = color,
                    darkIcons = when (style) {
                        BarStyle.Dark -> true
                        BarStyle.Light -> false
                        else -> color.luminance() > 0.5F
                    },
                )
                onDispose { }
            }
            var visible by visibleState
            DisposableEffect(visible) {
                systemUiController.isNavigationBarVisible = visible
                onDispose { }
            }

            observer.stateChanges.also {
                it.rememberByState(overlayState)
                it.rememberByState(colorState)
                it.rememberByState(styleState)
                it.rememberByState(visibleState)
                it.rememberByState(navigationBarsInsetsState)

                it.effectChange {
                    debugNativeUi("StatusBar", "CHANGED")
                    observer.notifyObserver()
                }
            }

            return this
        }


        data class NavigationBarState(
            val visible: Boolean,
            val style: BarStyle,
            val overlay: Boolean,
            val color: ColorJson,
            val boundingRect: RectJson,
        )

        fun toJsonAble() = NavigationBarState(
            visible = visibleState.value,
            style = styleState.value,
            overlay = overlayState.value,
            color = colorState.value.toJsonAble(),
            boundingRect = navigationBarsInsetsState.value.toJsonAble(),
        )
    }

    @Stable
    class VirtualKeyboardController(
        val activity: ComponentActivity,
        val nativeUiController: NativeUiController,
    ) {
        /**
         * 是否覆盖content
         */
        val overlayState = mutableStateOf(true)

        /**
         * 是否显示
         */
        val showState = mutableStateOf(false)

        val imeInsets = mutableStateOf(WindowInsets(0))

        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        fun effect(): VirtualKeyboardController {
            imeInsets.value = WindowInsets.ime

            val show by showState
            LocalSoftwareKeyboardController.current?.also { keyboard ->
                if (show) {
                    keyboard.show()
                } else {
                    keyboard.hide()
                }
            }

            return this
        }
    }

    /**
     * 安全区域，被 设备的顶部流海、状态栏、导航栏等原生UI所影响后，分割出inner、outer两个部分
     */
    class SafeAreaController(
        val activity: ComponentActivity, val nativeUiController: NativeUiController
    ) {
        /**
         * 刘海屏
         */
        val cutoutInsetsState = mutableStateOf(WindowInsets(0))

        /**
         * 是否要覆盖刘海屏
         */
        val overlayState = mutableStateOf(true)

        /**
         * 内部区域
         */
        val innerSafeAreaInsetsState = mutableStateOf(WindowInsets(0))

        /**
         * 外部区域
         */
        val outerAreaInsetsState = mutableStateOf(WindowInsets(0))

        val observer = StateObservable { gson.toJson(toJsonAble()) }

        @Composable
        fun effect(): SafeAreaController {
            cutoutInsetsState.value = WindowInsets.Companion.displayCutout

            val statusBar = nativeUiController.statusBar
            val virtualKeyboard = nativeUiController.virtualKeyboard
            val navigationBar = nativeUiController.navigationBar
            val safeArea = this

            observer.stateChanges.let {
                val isSafeAreaOverlay by it.rememberByState(safeArea.overlayState)
                val safeAreaCutoutInsets by it.rememberByState(safeArea.cutoutInsetsState)

                val isStatusBarOverlay by it.rememberByState(statusBar.overlayState)
                val statusBarsInsets by it.rememberByState(statusBar.statusBarsInsetsState)

                val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboard.overlayState)
                val imeInsets by it.rememberByState(virtualKeyboard.imeInsets)

                val isNavigationBarOverlay by it.rememberByState(navigationBar.overlayState)
                val navigationBarsInsets by it.rememberByState(navigationBar.navigationBarsInsetsState)

                it.effectChange {
                    var RES_safeAreaInsets = WindowInsets(0)
                    var RES_contentArea = WindowInsets(0)

                    /// 顶部
                    if (isStatusBarOverlay && isSafeAreaOverlay) {
                        // 都覆盖，那么就写入safeArea，contentArea不需要调整
                        RES_safeAreaInsets += safeAreaCutoutInsets.union(statusBarsInsets)
                    } else if (isStatusBarOverlay && !isSafeAreaOverlay) {
                        // safeArea只写入状态栏，contentArea写入剩余的
                        RES_safeAreaInsets += safeAreaCutoutInsets
                        RES_contentArea += safeAreaCutoutInsets.exclude(statusBarsInsets)
                    } else if (!isStatusBarOverlay && isSafeAreaOverlay) {
                        // safeArea只写入安全区域，contentArea写入剩余的
                        RES_safeAreaInsets += statusBarsInsets
                        RES_contentArea += statusBarsInsets.exclude(safeAreaCutoutInsets)
                    } else {
                        // 都不覆盖，全部写入 contentArea
                        RES_contentArea += safeAreaCutoutInsets.union(statusBarsInsets)
                    }
                    /// 底部
                    // 底部带键盘
                    if (isVirtualKeyboardOverlay && isNavigationBarOverlay) {
                        // 都覆盖，那么就写入safeArea，contentArea不需要调整
                        RES_safeAreaInsets += navigationBarsInsets.union(imeInsets)
                    } else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
                        // safeArea只写入键盘，contentArea写入剩余的
                        RES_safeAreaInsets += imeInsets
                        RES_contentArea += imeInsets.exclude(navigationBarsInsets)
                    } else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay) {
                        // safeArea只写入底部栏，contentArea写入剩余的
                        RES_safeAreaInsets += navigationBarsInsets
                        RES_contentArea += navigationBarsInsets.exclude(imeInsets)
                    } else if (!isVirtualKeyboardOverlay && !isNavigationBarOverlay) {

                        // 都不覆盖，全部写入 contentArea
                        RES_contentArea += navigationBarsInsets.union(imeInsets)
                    }

                    innerSafeAreaInsetsState.value = RES_safeAreaInsets
                    outerAreaInsetsState.value = RES_contentArea
                    debugNativeUi("SafeArea", "CHANGED")
                    observer.notifyObserver()
                }
            }
            return this
        }


        data class SafeAreaState(
            val cutoutRect: RectJson,
            val overlay: Boolean,
            val boundingOuterRect: RectJson,
            val boundingInnerRect: RectJson,
        )

        fun toJsonAble() = SafeAreaState(
            cutoutRect = cutoutInsetsState.value.toJsonAble(),
            overlay = overlayState.value,
            boundingOuterRect = outerAreaInsetsState.value.toJsonAble(),
            boundingInnerRect = innerSafeAreaInsetsState.value.toJsonAble(),
        )
    }

    companion object {
        init {
            QueryHelper.init() // 初始化
        }
    }

}

private operator fun WindowInsets.plus(safeAreaCutoutInsets: WindowInsets) =
    this.add(safeAreaCutoutInsets)


fun NativeUiController.Companion.fromMultiWebView(mmid: Mmid) =
    ((MultiWebViewNMM.getCurrentWebViewController(mmid)
        ?: throw Exception("native ui is unavailable for $mmid")).webViewList?.lastOrNull()
        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController

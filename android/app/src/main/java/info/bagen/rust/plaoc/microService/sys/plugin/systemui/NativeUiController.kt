package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import info.bagen.rust.plaoc.util.IsChange
import java.lang.reflect.Type


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("nativeui", tag, msg, err)

class NativeUiController(
    val activity: ComponentActivity,
) {

    val statusBar = StatusBarController(activity, this)
    val navigationBar = NavigationBarController(activity, this)
    val virtualKeyboard = VirtualKeyboardController(activity, this)

    //    val contentArea = ContentAreaController(activity, this)
    val safeArea = SafeAreaController(activity, this)


    @Composable
    fun effect(): NativeUiController {
        /**
         * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
         */
        SideEffect {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
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
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): BarStyle = from(json.asString)

        override fun serialize(
            src: BarStyle,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement = JsonPrimitive(src.style)

    }

//
//    companion object {
//
//        @OptIn(ExperimentalLayoutApi::class)
//        @Composable
//        fun remember(activity: ComponentActivity): NativeUiController {
//            val statusBarController = StatusBarController.remember(activity)
//            val navigationBarController = NavigationBarController.remember(activity)
//            val virtualKeyboardController = VirtualKeyboardController.remember(activity)
//
//            val modifierPaddingState = remember {
//                mutableStateOf(PaddingValues())
//            }
//
//            val modifierOffsetState = remember {
//                mutableStateOf(IntOffset(0, 0))
//            }
//
//            val modifierScaleState = remember {
//                mutableStateOf(Pair(0.0F, 0.0F))
//            }
//
//            val nativeUiController = remember {
//                NativeUiController(
//                    statusBarController = statusBarController,
//                    navigationBarController = navigationBarController,
//                    virtualKeyboardController = virtualKeyboardController,
//                    modifierPaddingState = modifierPaddingState,
//                    modifierOffsetState = modifierOffsetState,
//                    modifierScaleState = modifierScaleState,
//                )
//            }
//
//
////            val systemUiController = rememberSystemUiController()
//
////            val systemUi = rememberSystemUiController()
////            val useDarkIcons = !isSystemInDarkTheme()
////            DisposableEffect(systemUi, useDarkIcons) {
////                // 更新所有系统栏的颜色为透明
////                // 如果我们在浅色主题中使用深色图标
////                systemUi.setSystemBarsColor(
////                    color = Color.Transparent,
////                    darkIcons = useDarkIcons,
////                )
////                onDispose {}
////            }
//
//            /**
//             * 使用这个 SystemUIController，会使得默认覆盖 系统 UI
//             */
//            SideEffect {
//                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
//            }
//
//            // isSystemUILayoutChanged
//            rememberIsChange(true).let {
//
//                val isStatusBarOverlay by it.rememberByState(statusBarController.overlayState)
//                println("isStatusBarOverlay: $isStatusBarOverlay")
//                val statusBarsInsets by it.rememberToState(statusBarController.statusBarsInsets)
//                println("statusBarsInsets: $statusBarsInsets")
//
//                val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboardController.overlayState)
//                println("isVirtualKeyboardOverlay: $isVirtualKeyboardOverlay")
//                val isImeVisible by it.rememberToState(virtualKeyboardController.isImeVisible)
//                println("isImeVisible: $isImeVisible")
//                val imeInsets by it.rememberToState(virtualKeyboardController.imeInsets)
//                println("imeInsets: $imeInsets")
//
//                val isNavigationBarOverlay by it.rememberByState(navigationBarController.overlayState)
//                println("isNavigationBarOverlay: $isNavigationBarOverlay")
//                val navigationBarsInsets by it.rememberToState(navigationBarController.navigationBarsInsets)
//                println("navigationBarsInsets: $navigationBarsInsets")
//
//                it.effectChange {
//                    debugNativeUi(
//                        "LAYOUT-CHANGE", """
//                            isStatusBarOverlay: $isStatusBarOverlay
//                            statusBarsInsets: $statusBarsInsets
//                            isVirtualKeyboardOverlay: $isVirtualKeyboardOverlay
//                            isImeVisible: $isImeVisible
//                            imeInsets: $imeInsets
//                            isNavigationBarOverlay: $isNavigationBarOverlay
//                            navigationBarsInsets: $navigationBarsInsets
//                            """.trimIndent()
//                    )
//                    modifierPaddingState.value = WindowInsets(0).let {
//                        var res = it
//                        /// 顶部
//                        if (isStatusBarOverlay) {
//                        } else {
//                            res = res.add(statusBarsInsets)
//                        }
//                        /// 底部
//                        // 底部带键盘
//                        if (isVirtualKeyboardOverlay && isNavigationBarOverlay) {
//
//                        } else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
//                            res = res.add(navigationBarsInsets)
//                        } else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay) {
//                            res = res.add(imeInsets)
//                        } else if (!isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
//                            val density = LocalDensity.current
//                            val imeBottom = imeInsets.getBottom(density)
//                            val navBottom = navigationBarsInsets.getBottom(
//                                density
//                            )
//                            val bottomInsets = if (imeBottom > navBottom) imeInsets
//                            else navigationBarsInsets
//
//                            res = res.add(bottomInsets)
//                        }
//                        println("modifierPaddingState: $res")
//                        res
//                    }.asPaddingValues()
//                }
//            }
//
//
//            return nativeUiController
//        }
//    }

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
            val visible by visibleState
            DisposableEffect(color, style) {
                debugNativeUi(
                    "StatsBar", """
                    color: $color
                    style: $style
                    visible: $visible
                """.trimIndent()
                )
                systemUiController.setStatusBarColor(
                    color = color,
                    darkIcons = when (style) {
                        BarStyle.Dark -> true
                        BarStyle.Light -> false
                        else -> color.luminance() > 0.5F
                    },
                )
                systemUiController.isStatusBarVisible = visible
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
                    runBlockingCatching {
                        observer.changeSignal.emit()
                    }.getOrNull()
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

            val overlay by overlayState
            val color by colorState
            val style by styleState
            val visible by visibleState
            DisposableEffect(overlay, color, style, visible) {
                systemUiController.setNavigationBarColor(
                    color = color,
                    darkIcons = when (style) {
                        BarStyle.Dark -> true
                        BarStyle.Light -> false
                        else -> color.luminance() > 0.5F
                    },
                )
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
                    runBlockingCatching {
                        observer.changeSignal.emit()
                    }.getOrNull()
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
//            @Composable get() = WindowInsets.ime

//        @OptIn(ExperimentalLayoutApi::class)
//        val isImeVisible = mutableStateOf(false)
//            @Composable get() = WindowInsets.isImeVisible

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
//
//
//    class ContentAreaController(
//        val activity: ComponentActivity, val nativeUiController: NativeUiController
//    ) {
//        /**
//         * 边距，当我们与 system ui 不层叠渲染是（overlay=false），我们就需要通过 padding 来对我们的视图进行边距调整
//         * 它将会影响元素的内宽高
//         */
//        val modifierPaddingState = mutableStateOf(PaddingValues())
//
//        /**
//         * 偏移量，有时候我们并不想改变元素的宽高，而只是想进行一定程度的偏移
//         * 比如可以做一些警告的抖动特效
//         */
//        val modifierOffsetState = mutableStateOf(IntOffset(0, 0))
//
//        /**
//         * 缩放量
//         */
//        val modifierScaleState = mutableStateOf(Pair(0.0F, 0.0F))
//
//
//        private val isContentLayoutChanged = IsChange(true)
//
//        /**
//         * 使得当前 ContentController 生效
//         */
//        @Composable
//        fun effect(): ContentAreaController {
//            val statusBar = nativeUiController.statusBar
//            val virtualKeyboard = nativeUiController.virtualKeyboard
//            val navigationBar = nativeUiController.navigationBar
//            val safeArea = nativeUiController.safeArea
//
//            // isContentLayoutChanged
//            isContentLayoutChanged.let {
//                val isSafeAreaOverlay by it.rememberByState(safeArea.overlayState)
//                val safeAreaCutoutInsets by it.rememberByState(safeArea.cutoutInsetsState)
//
//                val isStatusBarOverlay by it.rememberByState(statusBar.overlayState)
//                val statusBarsInsets by it.rememberByState(statusBar.statusBarsInsetsState)
//
//                val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboard.overlayState)
//                val imeInsets by it.rememberByState(virtualKeyboard.imeInsets)
//
//                val isNavigationBarOverlay by it.rememberByState(navigationBar.overlayState)
//                val navigationBarsInsets by it.rememberByState(navigationBar.navigationBarsInsetsState)
//
//                it.effectChange {
//                    debugNativeUi(
//                        "LAYOUT-CHANGE", """
//                            isSafeAreaOverlay: $isSafeAreaOverlay
//                            safeAreaCutoutInsets: $safeAreaCutoutInsets
//                            isStatusBarOverlay: $isStatusBarOverlay
//                            statusBarsInsets: $statusBarsInsets
//                            isVirtualKeyboardOverlay: $isVirtualKeyboardOverlay
//                            imeInsets: $imeInsets
//                            isNavigationBarOverlay: $isNavigationBarOverlay
//                            navigationBarsInsets: $navigationBarsInsets
//                            """.trimIndent()
//                    )
//
//                    modifierPaddingState.value = WindowInsets(0).let {
//                        var res = it
//                        /// 顶部
//                        if (isStatusBarOverlay && isSafeAreaOverlay) {
//                        } else if (isStatusBarOverlay && !isSafeAreaOverlay) {
//                            res = res.add(statusBarsInsets)
//                        } else if (!isStatusBarOverlay && isSafeAreaOverlay) {
//                            res = res.add(safeAreaCutoutInsets)
//                        } else {
//                            val density = LocalDensity.current
//                            val cutoutTop = safeAreaCutoutInsets.getTop(density)
//                            val statusTop = statusBarsInsets.getTop(density)
//                            val topInsets =
//                                if (cutoutTop > statusTop) safeAreaCutoutInsets else statusBarsInsets
//
//                            res = res.add(topInsets)
//                        }
//                        /// 底部
//                        // 底部带键盘
//                        if (isVirtualKeyboardOverlay && isNavigationBarOverlay) {
//
//                        } else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
//                            res = res.add(navigationBarsInsets)
//                        } else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay) {
//                            res = res.add(imeInsets)
//                        } else if (!isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
//                            val density = LocalDensity.current
//                            val imeBottom = imeInsets.getBottom(density)
//                            val navBottom = navigationBarsInsets.getBottom(
//                                density
//                            )
//                            val bottomInsets =
//                                if (imeBottom > navBottom) imeInsets else navigationBarsInsets
//
//                            res = res.add(bottomInsets)
//                        }
//                        println("modifierPaddingState: $res")
//                        res
//                    }.asPaddingValues()
//                }
//            }
//
//            return this
//        }
//    }

    class SafeAreaController(
        val activity: ComponentActivity, val nativeUiController: NativeUiController
    ) {
        /**
         * 刘海屏
         */
        val cutoutInsetsState = mutableStateOf(WindowInsets(0))

        val overlayState = mutableStateOf(true)

        val safeAreaInsetsState = mutableStateOf(WindowInsets(0))
        val contentAreaInsetsState = mutableStateOf(WindowInsets(0))

        private val isSafeAreaRectChanged = IsChange(true)

        @Composable
        fun effect(): SafeAreaController {
            cutoutInsetsState.value = WindowInsets.Companion.displayCutout

            val statusBar = nativeUiController.statusBar
            val virtualKeyboard = nativeUiController.virtualKeyboard
            val navigationBar = nativeUiController.navigationBar
            val safeArea = this

            isSafeAreaRectChanged.let {
                val isSafeAreaOverlay by it.rememberByState(safeArea.overlayState)
                val safeAreaCutoutInsets by it.rememberByState(safeArea.cutoutInsetsState)

                val isStatusBarOverlay by it.rememberByState(statusBar.overlayState)
                val statusBarsInsets by it.rememberByState(statusBar.statusBarsInsetsState)

                val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboard.overlayState)
                val imeInsets by it.rememberByState(virtualKeyboard.imeInsets)

                val isNavigationBarOverlay by it.rememberByState(navigationBar.overlayState)
                val navigationBarsInsets by it.rememberByState(navigationBar.navigationBarsInsetsState)

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

                safeAreaInsetsState.value = RES_safeAreaInsets
                contentAreaInsetsState.value = RES_contentArea

            }
            return this
        }
    }

    companion object {

    }

}

private operator fun WindowInsets.plus(safeAreaCutoutInsets: WindowInsets) =
    this.add(safeAreaCutoutInsets)


fun NativeUiController.Companion.fromMultiWebView(mmid: Mmid) =
    ((MultiWebViewNMM.getCurrentWebViewController(mmid)
        ?: throw Exception("native ui is unavailable for $mmid")).webViewList?.lastOrNull()
        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController

package info.bagen.rust.plaoc.microService.sys.plugin.systemui


import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.util.rememberIsChange


inline fun debugSystemUi(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("systemui", tag, msg, err)

class SystemUiController(
    val statusBarController: StatusBarController,
    val navigationBarController: NavigationBarController,
    val virtualKeyboardController: VirtualKeyboardController,
    /**
     * 边距，当我们与 system ui 不层叠渲染是（overlay=false），我们就需要通过 padding 来对我们的视图进行边距调整
     * 它将会影响元素的内宽高
     */
    val modifierPaddingState: State<PaddingValues>,
    /**
     * 偏移量，有时候我们并不想改变元素的宽高，而只是想进行一定程度的偏移
     * 比如可以做一些警告的抖动特效
     */
    val modifierOffsetState: State<IntOffset>,
    /**
     * 缩放量
     */
    val modifierScaleState: State<Pair<Float, Float>>,
) {
    companion object {

        @OptIn(ExperimentalLayoutApi::class)
        @Composable
        fun remember(activity: ComponentActivity): SystemUiController {
            val statusBarController = StatusBarController.remember(activity)
            val navigationBarController = NavigationBarController.remember(activity)
            val virtualKeyboardController = VirtualKeyboardController.remember(activity)

            val modifierPaddingState = remember {
                mutableStateOf(PaddingValues())
            }

            val modifierOffsetState = remember {
                mutableStateOf(IntOffset(0, 0))
            }

            val modifierScaleState = remember {
                mutableStateOf(Pair(0.0F, 0.0F))
            }

            val systemUiController = remember {
                SystemUiController(
                    statusBarController = statusBarController,
                    navigationBarController = navigationBarController,
                    virtualKeyboardController = virtualKeyboardController,
                    modifierPaddingState = modifierPaddingState,
                    modifierOffsetState = modifierOffsetState,
                    modifierScaleState = modifierScaleState,
                )
            }


//            val systemUiController = rememberSystemUiController()

//            val systemUi = rememberSystemUiController()
//            val useDarkIcons = !isSystemInDarkTheme()
//            DisposableEffect(systemUi, useDarkIcons) {
//                // 更新所有系统栏的颜色为透明
//                // 如果我们在浅色主题中使用深色图标
//                systemUi.setSystemBarsColor(
//                    color = Color.Transparent,
//                    darkIcons = useDarkIcons,
//                )
//                onDispose {}
//            }

            /**
             * 使用这个 SystemUIController，会使得默认覆盖 系统 UI
             */
            SideEffect {
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            }

            // isSystemUILayoutChanged
            rememberIsChange(true).let {

                val isStatusBarOverlay by it.rememberByState(statusBarController.overlayState)
                println("isStatusBarOverlay: $isStatusBarOverlay")
                val statusBarsInsets by it.rememberToState(statusBarController.statusBarsInsets)
                println("statusBarsInsets: $statusBarsInsets")

                val isVirtualKeyboardOverlay by it.rememberByState(virtualKeyboardController.overlayState)
                println("isVirtualKeyboardOverlay: $isVirtualKeyboardOverlay")
                val isImeVisible by it.rememberToState(virtualKeyboardController.isImeVisible)
                println("isImeVisible: $isImeVisible")
                val imeInsets by it.rememberToState(virtualKeyboardController.imeInsets)
                println("imeInsets: $imeInsets")

                val isNavigationBarOverlay by it.rememberByState(navigationBarController.overlayState)
                println("isNavigationBarOverlay: $isNavigationBarOverlay")
                val navigationBarsInsets by it.rememberToState(navigationBarController.navigationBarsInsets)
                println("navigationBarsInsets: $navigationBarsInsets")

                it.effectChange {
                    debugSystemUi(
                        "LAYOUT-CHANGE", """
                            isStatusBarOverlay: $isStatusBarOverlay
                            statusBarsInsets: $statusBarsInsets
                            isVirtualKeyboardOverlay: $isVirtualKeyboardOverlay
                            isImeVisible: $isImeVisible
                            imeInsets: $imeInsets
                            isNavigationBarOverlay: $isNavigationBarOverlay
                            navigationBarsInsets: $navigationBarsInsets
                            """.trimIndent()
                    )
                    modifierPaddingState.value = WindowInsets(0).let {
                        var res = it
                        /// 顶部
                        if (isStatusBarOverlay) {
                        } else {
                            res = res.add(statusBarsInsets)
                        }
                        /// 底部
                        // 底部带键盘
                        if (isVirtualKeyboardOverlay && isNavigationBarOverlay) {

                        } else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
                            res = res.add(navigationBarsInsets)
                        } else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay) {
                            res = res.add(imeInsets)
                        } else if (!isVirtualKeyboardOverlay && !isNavigationBarOverlay) {
                            val density = LocalDensity.current
                            val imeBottom = imeInsets.getBottom(density)
                            val navBottom = navigationBarsInsets.getBottom(
                                density
                            )
                            val bottomInsets = if (imeBottom > navBottom) imeInsets
                            else navigationBarsInsets

                            res = res.add(bottomInsets)
                        }
                        println("modifierPaddingState: $res")
                        res
                    }.asPaddingValues()
                }
            }


            return systemUiController
        }
    }

    class StatusBarController(
        val overlayState: MutableState<Boolean>,
        val colorState: MutableState<Color>,
        val isDarkIconsState: MutableState<Boolean?>,
        val visibleState: MutableState<Boolean>,
    ) {
        val statusBarsInsets
            @Composable get() = WindowInsets.statusBars

        companion object {

            @Composable
            fun remember(activity: ComponentActivity): StatusBarController {
                val systemUiController = rememberSystemUiController()

                //region Color

                val isColorChanged = rememberIsChange(true)
                val color = isColorChanged.rememberToState(Color.Transparent)
                val isDarkIcons = isColorChanged.rememberToState<Boolean?>(!isSystemInDarkTheme())

                isColorChanged.effectChange {
                    debugSystemUi("StatusBar", "Color Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.setStatusBarColor(
                                color = color.value,
                                darkIcons = isDarkIcons.value ?: (color.value.luminance() > 0.5F),
                            )
                        }
                    }
                }

                //endregion

                //region Visible
                val isVisible = rememberIsChange(false)
                val visible =
                    isVisible.rememberToState(value = systemUiController.isStatusBarVisible)
                isVisible.effectChange {
                    debugSystemUi("StatusBar", "Visible Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.isStatusBarVisible = visible.value
                        }
                    }
                }
                //endregion


                return remember {
                    StatusBarController(
                        overlayState = mutableStateOf(true),
                        colorState = color,
                        isDarkIconsState = isDarkIcons,
                        visibleState = visible,
                    )
                }
            }
        }
    }

    @Stable
    class NavigationBarController(
        val overlayState: MutableState<Boolean>,
        val colorState: MutableState<Color>,
        val isDarkIconsState: MutableState<Boolean?>,
        val isContrastEnforcedState: MutableState<Boolean?>,
        val visibleState: MutableState<Boolean>,
    ) {
        val navigationBarsInsets
            @Composable get() = WindowInsets.navigationBars

        companion object {

            @Composable
            fun remember(activity: ComponentActivity): NavigationBarController {
                val systemUiController = rememberSystemUiController()

                //region Color

                val isColorChanged = rememberIsChange(true)
                val color = isColorChanged.rememberToState(Color.Transparent)
                val isDarkIcons = isColorChanged.rememberToState<Boolean?>(!isSystemInDarkTheme())
                val isContrastEnforced =
                    isColorChanged.rememberToState<Boolean?>(systemUiController.navigationBarDarkContentEnabled)
                isColorChanged.effectChange {
                    debugSystemUi("Navigation", "Color Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.setNavigationBarColor(
                                color = color.value,
                                darkIcons = isDarkIcons.value ?: (color.value.luminance() > 0.5F),
                                navigationBarContrastEnforced = isContrastEnforced.value ?: true,
                            )
                        }
                    }
                }

                //endregion

                //region Visible
                val isVisible = rememberIsChange(false)
                val visible =
                    isVisible.rememberToState(value = systemUiController.isNavigationBarVisible)
                isVisible.effectChange {
                    debugSystemUi("Navigation", "Visible Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.isNavigationBarVisible = visible.value
                        }
                    }
                }
                //endregion
                return remember {
                    NavigationBarController(
                        overlayState = mutableStateOf(true),
                        colorState = color,
                        isDarkIconsState = isDarkIcons,
                        isContrastEnforcedState = isContrastEnforced,
                        visibleState = visible,
                    )
                }
            }
        }
    }

    @Stable
    class VirtualKeyboardController(
        val overlayState: MutableState<Boolean>,
    ) {
        val imeInsets
            @Composable get() = WindowInsets.ime

        @OptIn(ExperimentalLayoutApi::class)
        val isImeVisible
            @Composable get() = WindowInsets.isImeVisible

        companion object {
            @Composable
            fun remember(activity: ComponentActivity): VirtualKeyboardController {
                return remember {
                    VirtualKeyboardController(
                        overlayState = mutableStateOf(true),
                    )
                }
            }
        }
    }

}

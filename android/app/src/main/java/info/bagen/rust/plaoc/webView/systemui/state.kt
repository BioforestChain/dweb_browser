package info.bagen.rust.plaoc.webView.systemui


import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.rust.plaoc.util.rememberIsChange

private const val TAG = "SystemUIState"


@Stable
data class SystemUIState(
    val statusBar: StatusBarState,
    val navigationBar: NavigationBarState,
    val virtualKeyboard: VirtualKeyboardState,
) {
    @Stable
    class StatusBarState(
        val overlay: MutableState<Boolean>,
        val color: MutableState<Color>,
        val isDarkIcons: MutableState<Boolean?>,
        val visible: MutableState<Boolean>,
    ) {
        companion object {

            @Composable
            fun Default(activity: ComponentActivity): StatusBarState {

                val systemUiController = rememberSystemUiController()

                //region Overlay
                val isOverlayChanged = rememberIsChange()
                val overlay = isOverlayChanged.rememberStateOf(false)
                isOverlayChanged.effectChange {
                    Log.i(TAG, "StatusBar Overlay Changed! ${overlay.value}")
                }
                //endregion

                //region Color

                val isColorChanged = rememberIsChange()
                val color = isColorChanged.rememberStateOf(Color.Unspecified)
                val isDarkIcons = isColorChanged.rememberStateOf<Boolean?>(null)

                isColorChanged.effectChange {
                    Log.i(TAG, "StatusBar Color Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.setStatusBarColor(
                                color = color.value,
                                darkIcons = isDarkIcons.value
                                    ?: (color.value.luminance() > 0.5F),
                            )
                        }
                    }
                }

                //endregion

                //region Visible
                val isVisible = rememberIsChange()
                val visible =
                    isVisible.rememberStateOf(value = systemUiController.isStatusBarVisible)
                isVisible.effectChange {
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.isStatusBarVisible = visible.value
                        }
                    }
                }
                //endregion


                return remember {
                    StatusBarState(
                        overlay = overlay,
                        color = color,
                        isDarkIcons = isDarkIcons,
                        visible = visible,
                    )
                }
            }
        }
    }

    @Stable
    class NavigationBarState(
        val overlay: MutableState<Boolean>,
        val color: MutableState<Color>,
        val isDarkIcons: MutableState<Boolean?>,
        val isContrastEnforced: MutableState<Boolean?>,
        val visible: MutableState<Boolean>,
    ) {
        companion object {

            @Composable
            fun Default(activity: ComponentActivity): NavigationBarState {

                val systemUiController = rememberSystemUiController()

                //region Overlay
                val isOverlayChanged = rememberIsChange()
                val overlay = isOverlayChanged.rememberStateOf(false)
                isOverlayChanged.effectChange {
                    Log.i(TAG, "Navigation Overlay Changed! ${overlay.value}")
                }
                //endregion

                //region Color

                val isColorChanged = rememberIsChange()
                val color = isColorChanged.rememberStateOf(Color.Unspecified)
                val isDarkIcons = isColorChanged.rememberStateOf<Boolean?>(null)
                val isContrastEnforced =
                    isColorChanged.rememberStateOf<Boolean?>(systemUiController.navigationBarDarkContentEnabled)
                isColorChanged.effectChange {
                    Log.i(TAG, "Navigation Color Changed!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.setNavigationBarColor(
                                color = color.value,
                                darkIcons = isDarkIcons.value
                                    ?: (color.value.luminance() > 0.5F),
                                navigationBarContrastEnforced = isContrastEnforced.value ?: true,
                            )
                        }
                    }
                }

                //endregion

                //region Visible
                val isVisible = rememberIsChange()
                val visible =
                    isVisible.rememberStateOf(value = systemUiController.isNavigationBarVisible)
                isVisible.effectChange {
                    Log.i(TAG, "isNavigationBarVisible!")
                    SideEffect {
                        activity.runOnUiThread {
                            systemUiController.isNavigationBarVisible = visible.value
                        }
                    }
                }
                //endregion


                return remember {
                    NavigationBarState(
                        overlay = overlay,
                        color = color,
                        isDarkIcons = isDarkIcons,
                        isContrastEnforced = isContrastEnforced,
                        visible = visible,
                    )
                }
            }
        }
    }

    @Stable
    class VirtualKeyboardState(
        val overlay: MutableState<Boolean>,
    ) {
        companion object {
            @Composable
            fun Default(): VirtualKeyboardState {
                return remember {
                    VirtualKeyboardState(
                        overlay = mutableStateOf(false),
                    )
                }
            }
        }
    }

    companion object {
        @Composable
        fun Default(activity: ComponentActivity): SystemUIState {
            val statusBar = StatusBarState.Default(activity)
            val navigationBar = NavigationBarState.Default(activity)
            val virtualKeyboard = VirtualKeyboardState.Default()
            return remember {
                SystemUIState(
                    statusBar = statusBar,
                    navigationBar = navigationBar,
                    virtualKeyboard = virtualKeyboard,
                )
            }
        }
    }
}

package info.bagen.rust.plaoc.webView.bottombar


import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.material.primarySurface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

@Stable
class BottomBarState(
    val enabled: MutableState<Boolean?>,
    val overlay: MutableState<Boolean?>,
    val alpha: MutableState<Float?>,
    val height: MutableState<Float?>,
    val actions: SnapshotStateList<BottomBarAction>,
    val backgroundColor: MutableState<Color>,
    val foregroundColor: MutableState<Color>,
) {
    val isEnabled: Boolean
        get() {
            return if (enabled.value == null) {
                actions.size > 0
            } else {
                enabled.value as Boolean
            }
        }

    companion object {
        @Composable
        fun Default(): BottomBarState {
            val bottomBarBackgroundColor =
                MaterialTheme.colors.primarySurface.let { defaultPrimarySurface ->
                    remember {
                        mutableStateOf(defaultPrimarySurface)
                    }
                }
            val bottomBarForegroundColor =
                MaterialTheme.colors.contentColorFor(bottomBarBackgroundColor.value)
                    .let { defaultContentColor ->
                        remember {
                            mutableStateOf(defaultContentColor)
                        }
                    }
            return remember {
                BottomBarState(
                    enabled = mutableStateOf(null),
                    overlay = mutableStateOf(false),
                    alpha = mutableStateOf(1.0F),
                    height = mutableStateOf(0F),
                    actions = mutableStateListOf(),
                    backgroundColor = bottomBarBackgroundColor,
                    foregroundColor = bottomBarForegroundColor,
                )
            }
        }
    }
}

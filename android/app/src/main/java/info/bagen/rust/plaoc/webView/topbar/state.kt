package info.bagen.rust.plaoc.webView.topbar


import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

@Stable
class TopBarState(
    val enabled: MutableState<Boolean>,
    val overlay: MutableState<Boolean?>,
    val alpha: MutableState<Float?>,
    val title: MutableState<String?>,
    var actions: SnapshotStateList<TopBarAction>,
    val foregroundColor: MutableState<Color>,
    val backgroundColor: MutableState<Color>,
    val height: MutableState<Float>,
    val doBack: () -> Unit,
) {
    companion object {
        @Composable
        fun Default(doBack: () -> Unit): TopBarState {

            val backgroundColor =
                MaterialTheme.colors.primaryVariant.let { defaultPrimarySurface ->
                    remember {
                        mutableStateOf(defaultPrimarySurface)
                    }
                }
            // https://developer.android.com/jetpack/compose/themes/material#emphasis
            // Material Design 文本易读性建议一文建议通过不同的不透明度来表示不同的重要程度。
            // TopAppBar 组件内部已经强制写死了 CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high)
            val foregroundColor =
                MaterialTheme.colors.contentColorFor(backgroundColor.value)
                    .let { defaultContentColor ->
                        remember {
                            mutableStateOf(defaultContentColor)
                        }
                    }


            return remember(doBack) {
                TopBarState(
                    enabled = mutableStateOf(true),
                    overlay = mutableStateOf(false),
                    alpha = mutableStateOf(1.0F),
                    title = mutableStateOf(null),
                    actions = mutableStateListOf(),
                    foregroundColor = foregroundColor,
                    backgroundColor = backgroundColor,
                    height = mutableStateOf(0F),
                    doBack = doBack,
                )
            }
        }
    }

}

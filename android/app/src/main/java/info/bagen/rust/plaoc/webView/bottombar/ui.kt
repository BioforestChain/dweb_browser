package info.bagen.rust.plaoc.webView.bottombar

import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import info.bagen.rust.plaoc.webView.icon.DWebIcon
import info.bagen.rust.plaoc.webView.jsutil.JsUtil

@Composable
fun DWebBottomBar(
    jsUtil: JsUtil?,
    bottomBarState: BottomBarState,
) {
    val localDensity = LocalDensity.current
    NavigationBar(
        modifier = Modifier
            .let {
                bottomBarState.height.value?.let { height ->
                    if (height >= 0) {
                        it.height(height.dp)
                    } else {
                        it
                    }
                } ?: it
            }
            .onGloballyPositioned { coordinates ->
                bottomBarState.height.value = coordinates.size.height / localDensity.density
            },
        containerColor = bottomBarState.backgroundColor.value.copy(
            bottomBarState.alpha.value ?: 1F
        ),
        contentColor = bottomBarState.foregroundColor.value,
        tonalElevation = 0.dp,
    ) {
        if (bottomBarState.actions.size > 0) {
            var selectedItem by remember(bottomBarState.actions) {
                mutableStateOf(bottomBarState.actions.indexOfFirst { it.selected }
                    .let { if (it == -1) 0 else it })
            }

            bottomBarState.actions.forEachIndexed { index, action ->
                NavigationBarItem(
                    icon = {
                        DWebIcon(action.icon)
                    },
                    alwaysShowLabel = !action.alwaysShowLabel,
                    label = if (action.label.isNotEmpty()) {
                        { Text(action.label) }
                    } else {
                        null
                    },
                    enabled = !action.disabled,
                    onClick = {
                        if (action.selectable) {
                            selectedItem = index
                        }
                        jsUtil?.evalQueue { action.onClickCode }
                    },
                    selected = selectedItem == index,
                    colors = action.colors?.toNavigationBarItemColors()
                        ?: NavigationBarItemDefaults.colors(),
                )
            }
        }
    }
}

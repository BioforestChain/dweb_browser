package org.dweb_browser.helper.compose.reorder

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberReorderAbleLazyGridState(
    onMove: (ItemPosition, ItemPosition) -> Unit,
    gridState: LazyGridState = rememberLazyGridState(),
    canDragOver: ((draggedOver: ItemPosition, dragging: ItemPosition) -> Boolean)? = null,
    onDragEnd: ((startIndex: Int, endIndex: Int) -> (Unit))? = null,
    maxScrollPerFrame: Dp = 20.dp,
    dragCancelledAnimation: DragCancelledAnimation = SpringDragCancelledAnimation()
): ReorderAbleLazyGridState {
    val maxScroll = with(LocalDensity.current) { maxScrollPerFrame.toPx() }
    val scope = rememberCoroutineScope()
    val state = remember(gridState) {
        ReorderAbleLazyGridState(gridState, scope, maxScroll, onMove, canDragOver, onDragEnd, dragCancelledAnimation)
    }
    LaunchedEffect(state) {
        state.visibleItemsChanged()
            .collect { state.onDrag(0, 0) }
    }

    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            gridState.scrollBy(diff)
        }
    }
    return state
}

class ReorderAbleLazyGridState(
    val gridState: LazyGridState,
    scope: CoroutineScope,
    maxScrollPerFrame: Float,
    onMove: (fromIndex: ItemPosition, toIndex: ItemPosition) -> (Unit),
    canDragOver: ((draggedOver: ItemPosition, dragging: ItemPosition) -> Boolean)? = null,
    onDragEnd: ((startIndex: Int, endIndex: Int) -> (Unit))? = null,
    dragCancelledAnimation: DragCancelledAnimation = SpringDragCancelledAnimation()
) : ReorderableState<LazyGridItemInfo>(scope, maxScrollPerFrame, onMove, canDragOver, onDragEnd, dragCancelledAnimation) {
    override val isVerticalScroll: Boolean
        get() = gridState.layoutInfo.orientation == Orientation.Vertical
    override val LazyGridItemInfo.left: Int
        get() = offset.x
    override val LazyGridItemInfo.right: Int
        get() = offset.x + size.width
    override val LazyGridItemInfo.top: Int
        get() = offset.y
    override val LazyGridItemInfo.bottom: Int
        get() = offset.y + size.height
    override val LazyGridItemInfo.width: Int
        get() = size.width
    override val LazyGridItemInfo.height: Int
        get() = size.height
    override val LazyGridItemInfo.itemIndex: Int
        get() = index
    override val LazyGridItemInfo.itemKey: Any
        get() = key
    override val visibleItemsInfo: List<LazyGridItemInfo>
        get() = gridState.layoutInfo.visibleItemsInfo
    override val viewportStartOffset: Int
        get() = gridState.layoutInfo.viewportStartOffset
    override val viewportEndOffset: Int
        get() = gridState.layoutInfo.viewportEndOffset
    override val firstVisibleItemIndex: Int
        get() = gridState.firstVisibleItemIndex
    override val firstVisibleItemScrollOffset: Int
        get() = gridState.firstVisibleItemScrollOffset

    override suspend fun scrollToItem(index: Int, offset: Int) {
        gridState.scrollToItem(index, offset)
    }
}
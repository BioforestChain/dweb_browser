package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import org.dweb_browser.helper.getOrDefault
import kotlin.math.abs

class DeskLayoutStateV6<T : Any>(
  val recommendLayout: (screenWidth: Int) -> Map<T, NFSpaceCoordinateLayout>,
  val resortLayout: (layoutWith: Int, layouts: Map<T, NFSpaceCoordinateLayout>) -> Unit
) {

  internal var calculatorParams by mutableStateOf(getCalculatorParams(0, 0))

  internal var blockLayouts by mutableStateOf<List<NFLayoutData<T>>>(emptyList())

  internal var flowLayouts by mutableStateOf<List<NFLayoutData<T>>>(emptyList())

  fun refresh(list: List<T>, screenWith: Int, screenHeight: Int) {
    calculatorParams = getCalculatorParams(screenWith, screenHeight)
    val recommendLayouts = recommendLayout(calculatorParams.screenWidth)
    flowLayouts = list.map { data ->
      val scLayout = recommendLayouts.getOrDefault(data, NFSpaceCoordinateLayout(0, 0, 1, 1))
      NFCaculater.getLayout(data, scLayout, calculatorParams)
    }
    calculateBlockLayout()
  }

  internal fun exportDatasGeometryMap() {
    val r = flowLayouts.associate { layout ->
      Pair(layout.data, layout.sCGeo)
    }
    resortLayout(calculatorParams.screenWidth, r)
  }

  fun calculateBlockLayout(): List<NFLayoutData<T>> {
    val blockKeys = blockLayouts.map {
      it.key
    }

    flowLayouts = NFCaculater.layout(
      layouts = flowLayouts.filter { !blockKeys.contains(it.key) },
      blockLayouts = blockLayouts,
      params = calculatorParams,
      refresh = false
    )
    return flowLayouts + blockLayouts
  }

  fun getCalculatorParams(width: Int, height: Int): NFCacalaterParams {
    return getLayoutParams(width, height)
  }

  fun getContainerBoxGeometry(width: Int, height: Int): Pair<IntOffset, Int> {
    val caculatorParams = getLayoutParams(width, height)
    val offset = IntOffset((width - caculatorParams.screenWidth) / 2, 0)
    return Pair(offset, caculatorParams.screenWidth)
  }

  fun doDragStart(dragLayout: NFLayoutData<T>) {
    blockLayouts += dragLayout
  }

  fun doDragMoveAction(
    draggingLayout: NFLayoutData<T>,
    dragOffset: IntOffset,
  ) {
    val blockLayout = NFCaculater.searchAreas(dragOffset, draggingLayout, calculatorParams)
    if (blockLayout != draggingLayout) {
      blockLayouts = blockLayouts.filter { it.data != draggingLayout.data } + blockLayout
    }
  }

  fun doDragEndAction() {
    flowLayouts += blockLayouts
    blockLayouts = emptyList()
  }
}

@Composable
fun <T : Any> DeskLayoutStateV6<T>.Render(
  modifier: Modifier,
  edit: Boolean,
  content: @Composable (T, NFGeometry, Boolean) -> Unit
) {
  BoxWithConstraints(modifier) {
    val screenWidth = constraints.maxWidth
    val screenHeight = constraints.maxHeight

    val containerBoxLayoutParams by remember(screenWidth, screenHeight) {
      mutableStateOf(getContainerBoxGeometry(screenWidth, screenHeight))
    }

    var layouts by remember {
      mutableStateOf(flowLayouts)
    }

    LaunchedEffect(flowLayouts) {
      layouts = flowLayouts + blockLayouts
    }

    LaunchedEffect(blockLayouts) {
      layouts = calculateBlockLayout()
    }

    val contentHeight = remember(layouts) {
      layouts.maxOfOrNull { it.geo.offset.y + it.geo.size.height }?.toInt() ?: 500
    }

    val scrollState = rememberScrollState()

    Box(
      modifier = Modifier.fillMaxSize()
        .offset { containerBoxLayoutParams.first }
        .width(containerBoxLayoutParams.second.dp).verticalScroll(scrollState)
        .height(contentHeight.div(LocalDensity.current.density).dp)
    ) {
      layouts.forEach { rlayout ->
        key(rlayout.key) {
          DraggableItem(rlayout, scrollState, edit, content)
        }
      }
    }
  }
}

@Composable
private fun <T : Any> DeskLayoutStateV6<T>.DraggableItem(
  rlayout: NFLayoutData<T>,
  scrollState: ScrollState,
  edit: Boolean,
  content: @Composable (T, NFGeometry, Boolean) -> Unit
) {
  val density = LocalDensity.current.density
  val layout by rememberUpdatedState(rlayout)

  // dragOffset：相对与整个box的布局偏移量。
  var dragOffset by remember(layout.key) { mutableStateOf(IntOffset.Zero) }
  var startDragScrollOffY by remember { mutableStateOf(0) }
  val dragging = remember(blockLayouts, layout) {
    blockLayouts.find { it.data == layout.data } != null
  }

  var layoutStartOffset by remember { mutableStateOf(IntOffset.Zero) }
  var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
  var draggingOffset by remember { mutableStateOf(Offset.Zero) }

  if (dragging) {
    val scrollV = draggingOffset.y - dragStartOffset.y
    if (abs(scrollV / density) > 25) {
      LaunchedEffect(scrollV) {
        while (true) {
          scrollState.animateScrollBy(scrollV, tween(500, easing = LinearEasing))
        }
      }
    }

    dragOffset = layoutStartOffset + // 起始位置
        IntOffset(0, scrollState.value - startDragScrollOffY) + // 滚动差异
        (draggingOffset - dragStartOffset).toInt() // 拖动差异

    val changed by rememberThrottleUpdatedState(dragOffset, 500)

    LaunchedEffect(changed) {
      doDragMoveAction(
        layout,
        dragOffset,
      )
    }
  }

  val animationOffset by animateIntOffsetAsState(
    if (dragging) {
      dragOffset
    } else {
      layout.geo.offset
    },
    if (dragging) {
      tween(0)
    } else {
      spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
    }
  )

  var boxModifier = Modifier.offset { animationOffset }
    .size(layout.geo.size.width.div(density).dp, layout.geo.size.height.div(density).dp)
    .zIndex(if (dragging) 1.0f else 0.0f)

  boxModifier = if (edit) {
    boxModifier.pointerInput(layout.key, calculatorParams) {
      detectDragGesturesAfterLongPress(
        onDragStart = { off ->
          dragStartOffset = off
          draggingOffset = dragStartOffset
          startDragScrollOffY = scrollState.value
          layoutStartOffset = layout.geo.offset
          doDragStart(layout)
        },
        onDrag = { change, dragAmount ->
          draggingOffset += dragAmount
        },
        onDragEnd = {
          dragOffset = IntOffset.Zero
          doDragEndAction()
        },
      )
    }
  } else {
    boxModifier
  }

//  if (dragging) {
//    DragPlaceIndicator(layout)
//  }

  Box(modifier = boxModifier) {
    content(layout.data, layout.geo, dragging)
  }

}

@Composable
fun <T> DragPlaceIndicator(layout: NFLayoutData<T>) {
  val density = LocalDensity.current.density

  Box(
    modifier = Modifier.offset {
      layout.geo.offset
    }.size(
      layout.geo.size.width.div(density).dp,
      layout.geo.size.height.div(density).dp
    )
      .border(1.dp, Color.White, RoundedCornerShape(16))
  )
}

@Composable
fun <T : Any> DeskLayoutV6(
  datas: List<T>,
  modifier: Modifier,
  contentPadding: PaddingValues,
  edit: Boolean,
  layout: (screenWidth: Int) -> Map<T, NFSpaceCoordinateLayout>,
  relayout: (screenWidth: Int, geoMaps: Map<T, NFSpaceCoordinateLayout>) -> Unit,
  content: @Composable (T, NFGeometry, Boolean) -> Unit
) {
  BoxWithConstraints(Modifier.padding(contentPadding)) {
    var isNeedSaveNextChanged by remember { mutableStateOf(false) }

    val desk = remember(datas, constraints.maxWidth) {
      DeskLayoutStateV6(layout, relayout).apply {
        refresh(datas, constraints.maxWidth, constraints.maxHeight)
      }
    }

    LaunchedEffect(edit) {
      if (isNeedSaveNextChanged) {
        desk.exportDatasGeometryMap()
        isNeedSaveNextChanged = false
      }
      isNeedSaveNextChanged = edit
    }
    desk.Render(modifier, edit, content)
  }
}

@Composable
fun <T> rememberThrottleUpdatedState(value: T, ms: Long): State<Any?> {
  val stateFlow = remember { MutableSharedFlow<T>() }
  LaunchedEffect(value) {
    stateFlow.emit(value)
  }
  return remember { stateFlow.conflate().map { delay(ms);it } }.collectAsState(value)
}
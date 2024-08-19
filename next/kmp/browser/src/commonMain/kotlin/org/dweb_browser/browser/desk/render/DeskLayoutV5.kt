package org.mkdesklayout.project

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Composable
fun rememberDeskLayout(screenWidth: Int) = remember {
  DeskLayoutStateV5(screenWidth)
}

private typealias DataValueType = String

class DeskLayoutStateV5(val screenWidth: Int) {

  private val datas = mutableListOf<NFData<DataValueType>>()

  private val datasGeometryMap = mutableMapOf<Int, NFGeometry>()

  var calculatorParams by mutableStateOf(getCalculatorParams(0, 0))

  var blockLayouts by mutableStateOf<List<NFLayoutData<DataValueType>>>(emptyList())

  var flowLayouts by mutableStateOf<List<NFLayoutData<DataValueType>>>(emptyList())

  init {
    //1.get datas
    datas.addAll(
      listOf(
        NFData("T11-0", NFDataType(1, 1)),
        NFData("T11-1", NFDataType(1, 1)),
        NFData("T11-2", NFDataType(1, 1)),
        NFData("T11-3", NFDataType(1, 1)),
        NFData("T11-4", NFDataType(1, 1)),
        NFData("T11-5", NFDataType(1, 1)),
        NFData("T11-6", NFDataType(1, 1)),
        NFData("T11-7", NFDataType(1, 1)),
        NFData("T11-8", NFDataType(1, 1)),
        NFData("T11-9", NFDataType(1, 1)),
        NFData("T11-10", NFDataType(1, 1)),
        NFData("T11-11", NFDataType(1, 1)),
        NFData("T11-12", NFDataType(1, 1)),
        NFData("T11-13", NFDataType(1, 1)),
        NFData("T11-14", NFDataType(1, 1)),

        NFData("T22-1", NFDataType(2, 2)),
        NFData("T22-2", NFDataType(2, 2)),
        NFData("T22-3", NFDataType(2, 2)),
        NFData("T22-4", NFDataType(2, 2)),
        NFData("T22-5", NFDataType(2, 2)),
        NFData("T22-6", NFDataType(2, 2)),

        NFData("T11-15", NFDataType(1, 1)),
        NFData("T11-16", NFDataType(1, 1)),
        NFData("T11-17", NFDataType(1, 1)),
        NFData("T11-18", NFDataType(1, 1)),
        NFData("T11-19", NFDataType(1, 1)),
        NFData("T11-20", NFDataType(1, 1)),
        NFData("T11-21", NFDataType(1, 1)),
        NFData("T11-22", NFDataType(1, 1)),
        NFData("T11-23", NFDataType(1, 1)),
        NFData("T11-24", NFDataType(1, 1)),
        NFData("T11-25", NFDataType(1, 1)),
        NFData("T11-26", NFDataType(1, 1)),
        NFData("T11-27", NFDataType(1, 1)),
        NFData("T11-28", NFDataType(1, 1)),
        NFData("T11-29", NFDataType(1, 1)),
        NFData("T11-30", NFDataType(1, 1)),

        NFData("T24-1", NFDataType(2, 4)),
        NFData("T24-2", NFDataType(2, 4)),
        NFData("T24-3", NFDataType(2, 4)),
        NFData("T24-4", NFDataType(2, 4)),

        NFData("T44-1", NFDataType(4, 4)),
        NFData("T44-2", NFDataType(4, 4)),
        NFData("T44-3", NFDataType(4, 4)),
        NFData("T44-4", NFDataType(4, 4)),
        NFData("T44-5", NFDataType(4, 4)),
        NFData("T44-6", NFDataType(4, 4)),
        )
    )

    //2.get datasGeoMap


    //3.assgin geometry
    flowLayouts = datas.map { data ->
      datasGeometryMap[data.hashCode()]?.let { geo ->
        NFLayoutData(data, geo)
      } ?: NFLayoutData(data, NFGeometry(IntOffset.Zero, IntSize.Zero))
    }
  }

  fun saveDatasGeometryMap() {
    datasGeometryMap.clear()
    flowLayouts.forEach {
      datasGeometryMap[it.data.key] = it.geo
    }
  }

  private val _layouts: List<NFLayoutData<DataValueType>>
    get() = flowLayouts + blockLayouts

  fun calculateLayout(): List<NFLayoutData<DataValueType>> {
    flowLayouts = NFCaculater.layout(
      layouts = datas.map { NFLayoutData(it, NFGeometry(IntOffset.Zero, IntSize.Zero)) },
      blockAreas = blockLayouts.map { it.geo },
      params = calculatorParams,
      refresh = true
    )
    return _layouts
  }


  fun calculateBlockLayout(): List<NFLayoutData<DataValueType>> {
    val blockKeys = blockLayouts.map {
      it.data.key
    }

    flowLayouts = NFCaculater.layout(
      layouts = flowLayouts.filter { !blockKeys.contains(it.data.key) },
      blockAreas = blockLayouts.map { it.geo },
      params = calculatorParams,
      refresh = false
    )
    return _layouts
  }

  fun getCalculatorParams(width: Int, height: Int): NFCacalaterParams {
    return getLayoutParams(width, height)
  }

  fun getContainerBoxGeometry(width: Int, height: Int): Pair<IntOffset, Int> {
    val caculatorParams = getLayoutParams(width, height)
    val offset = IntOffset((width - caculatorParams.screenWidth) / 2, 0)
    return Pair(offset, caculatorParams.screenWidth)
  }

  fun doDragStart(dragLayout: NFLayoutData<DataValueType>) {
    blockLayouts += dragLayout
  }

  fun doDragMoveAction(
    draggingLayout: NFLayoutData<DataValueType>,
    dragOffset: IntOffset,
  ) {
    val blockGeo = NFCaculater.searchAreas1(dragOffset, draggingLayout.geo.size, calculatorParams)
    if (blockGeo != draggingLayout.geo) {
      val newBlockLayout = draggingLayout.copy(geo = blockGeo)
      blockLayouts = blockLayouts.filter { it.data != draggingLayout.data } + newBlockLayout
    }
  }

  fun doDragEndAction() {
    flowLayouts += blockLayouts
    blockLayouts = emptyList()
    saveDatasGeometryMap()
  }
}

@Composable
fun DeskLayoutStateV5.Render(modifier: Modifier) {

  BoxWithConstraints(modifier) {
    val screenWidth = constraints.maxWidth
    val screenHeight = constraints.maxHeight
    val density = LocalDensity.current.density

    val containerBoxLayoutParams by remember(screenWidth, screenHeight) {
      mutableStateOf(getContainerBoxGeometry(screenWidth, screenHeight))
    }

    calculatorParams = remember(screenWidth, screenHeight) {
      getCalculatorParams(screenWidth, screenHeight)
    }


    var layouts by remember {
      mutableStateOf(calculateBlockLayout())
    }

    LaunchedEffect(calculatorParams) {
      layouts = calculateLayout()
    }

    LaunchedEffect(blockLayouts) {
      layouts = calculateBlockLayout()
    }

    val contentHeight = remember(layouts) {
      layouts.maxOfOrNull { it.geo.offset.y + it.geo.size.height }?.toInt() ?: 500
    }

    val scrollState = rememberScrollState()

    Box(
      modifier = Modifier.offset {
        containerBoxLayoutParams.first
      }.width(containerBoxLayoutParams.second.dp)
        .verticalScroll(scrollState)
        .height(contentHeight.div(LocalDensity.current.density).dp)
    ) {

      layouts.forEach { rlayout ->

        key(rlayout.key) {
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
            if (abs(scrollV / density) > calculatorParams.itemSize.width) {
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

          val modifierOffset = when {
            dragging -> dragOffset
            else -> animateIntOffsetAsState(
              layout.geo.offset,
              spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
            ).value
          }
          Box(
            modifier = Modifier.offset { modifierOffset }
              .size(layout.geo.size.width.div(density).dp, layout.geo.size.height.div(density).dp)
              .zIndex(if (dragging) 1.0f else 0.0f)
              .pointerInput(layout.key, calculatorParams, screenHeight) {
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
              },
          ) {
            DeskItemRender(Modifier, dragging, layout)
          }

          if (dragging) {
            Box(modifier = Modifier
              .offset {
                layout.geo.offset
              }
              .size(layout.geo.size.width.div(density).dp, layout.geo.size.height.div(density).dp)
              .zIndex( 1.0f)
              .clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(0.2f)))
          }
        }
      }
    }
  }
}

@Composable
fun DeskItemRender(modifier: Modifier, draging: Boolean, layout: NFLayoutData<DataValueType>) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxSize().border(2.dp, if (draging) Color.Red else Color.Black)
      .clip(RoundedCornerShape(8.dp)).background(Color.Yellow.copy(alpha = 0.5f))
  ) {
    Text(layout.data.value, modifier)
    Text("${layout.geo.offset}, ${layout.geo.size}", fontSize = 10.sp)
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

fun getLayoutParams(width: Int, height: Int): NFCacalaterParams {
  val column = if (width > height) 8 else 4
  return NFCacalaterParams(column, width, 10, 10)
}

//expect fun getLayoutParams(width: Int, height: Int): NFCacalaterParams

//actual fun getLayoutParams(width: Int, height: Int): NFCacalaterParams {
//  val space = 10
//  val itemW = 100
//  var column = width / (itemW + space)
//  var reminder = width - column * itemW - space * (column - 1).coerceAtLeast(0)
//  if (reminder > itemW) {
//    column++
//    reminder -= itemW
//  }
//  if (column < 4) {
//    column = 4
//    reminder = 0
//  }
//  println("FUUU getLayoutParams, width: $width, column: $column, reminder: $reminder, sw: ${width - reminder}")
//  return NFCacalaterParams(column, width - reminder, space, space)
//}

//actual fun getLayoutParams(width: Int, height: Int): NFCacalaterParams {
//  val column = if (width > height) 8 else 4
//  return NFCacalaterParams(column, width, 10, 10)
//}
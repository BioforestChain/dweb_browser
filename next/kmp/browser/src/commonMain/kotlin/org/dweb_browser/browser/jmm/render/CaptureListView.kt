package org.dweb_browser.browser.jmm.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.pure.image.compose.CoilAsyncImage
import kotlin.math.max

private val imageSizeCache = mutableMapOf<String, GridItem<String>>()

class CaptureItemContext(val src: String, val index: Int)

/**
 * 应用介绍的图片展示部分
 */
@Composable
internal fun CaptureListView(
  jmmAppInstallManifest: JmmAppInstallManifest,
//  decorationBox: @Composable CaptureItemContext.(innerItem: @Composable (modifier: Modifier) -> Unit) -> Unit = { innerItem ->
//    innerItem(Modifier)
//  }
  itemContent: @Composable CaptureItemContext.() -> Unit
) {
  val waterfallItems = remember(jmmAppInstallManifest.images) {
    jmmAppInstallManifest.images.map {
      imageSizeCache.getOrPut(it) {
        GridItem(it, mutableStateOf(IntSize(1, 1)))
      }
    }
  }
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val cells = max(1, (maxWidth.value / 160).toInt())
    val layoutList =
      waterfallCalc(columns = cells, items = waterfallItems.map { it.sizeState.value })
    val unitSize = maxWidth / cells
    val gridHeight = unitSize * (waterfallItems.mapIndexed { index, gridItem ->
      gridItem.sizeState.value.height + (layoutList?.getOrNull(index)?.y ?: 0)
    }.maxOrNull() ?: 0)

    Box(Modifier.fillMaxWidth().requiredHeight(gridHeight)) {
      waterfallItems.forEachIndexed { index, gridItem ->
        LocalCompositionChain.current.Provider(LocalSizeState provides gridItem.sizeState) {
          val layout = layoutList?.get(index) ?: IntOffset(0, 0)
          Box(
            Modifier.offset(unitSize * layout.x, unitSize * layout.y)
              .requiredSize(with(gridItem.sizeState.value) {
                DpSize(unitSize * width, unitSize * height)
              }).animateContentSize().padding(8.dp)
          ) {
            CaptureItemContext(src = gridItem.key, index = index).itemContent()
          }
        }
      }
    }
  }
}

internal val LocalSizeState = compositionChainOf<MutableState<IntSize>?>("sizeState") { null }

@Composable
internal fun CaptureImage(
  onClick: () -> Unit,
  src: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
) {
  val sizeState = LocalSizeState.current
  Card(
    onClick = onClick, modifier = modifier.fillMaxSize()
  ) {
    CoilAsyncImage(
      model = src,
      modifier = Modifier.fillMaxWidth(),
      contentDescription = null,
      contentScale = contentScale,
      onState = {
        it.painter?.apply {
          sizeState?.apply {
            value = when (intrinsicSize.width / intrinsicSize.height) {
              in 0f..0.75f -> IntSize(1, 2)
              in 0.75f..1.25f -> IntSize(1, 1)
              in 1.25f..Float.MAX_VALUE -> IntSize(2, 1)
              else -> IntSize(1, 1)
            }
          }
        }
      },
    )
  }
}


class CaptureBigImage() {
  var src by mutableStateOf<String?>(null)

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  fun Render(sharedTransitionScope: SharedTransitionScope) {
    with(sharedTransitionScope) {
      AnimatedContent(
        targetState = src,
        modifier = Modifier.fillMaxWidth().padding(bottom = AppBottomHeight),
        label = "Detail Image",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = Alignment.Center,
      ) { aniSrc ->
        /// 大图预览
        aniSrc?.also { currentSrc ->
          val alphaAni = remember { Animatable(0f) }
          LaunchedEffect(aniSrc, transition.isRunning) {
            if (src == null) {
              alphaAni.snapTo(0f)
            } else if (!transition.isRunning) {
              alphaAni.animateTo(1f)
            }
          }

          var scale by remember { mutableStateOf(1f) }
          var offset by remember { mutableStateOf(Offset.Zero) }
          Box(Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = alphaAni.value * 0.5f))
            .sharedBounds(rememberSharedContentState(key = "bounds:$currentSrc"), this)
            .renderInSharedTransitionScopeOverlay(
              renderInOverlay = { true }, zIndexInOverlay = 3f
            ).pointerInput(Unit) {
              detectTransformGestures { _, pan, zoom, _ ->
                scale *= zoom
                scale = clamp(0.5f, scale, 5f)
                offset += pan
              }
            }) {
            Box(
              Modifier.align(Alignment.BottomCenter).alpha(alphaAni.value).zIndex(2f)
            ) {
              FilledTonalIconButton(
                onClick = { src = null },
                modifier = Modifier.align(Alignment.Center),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "close image view")
              }
            }
            Box(Modifier.zIndex(1f).align(Alignment.Center).fillMaxSize().padding(24.dp)) {
              CoilAsyncImage(
                model = src,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                  scaleX = scale,
                  scaleY = scale,
                  translationX = offset.x,
                  translationY = offset.y,
                ).sharedElement(
                  rememberSharedContentState(key = "element:$currentSrc"), this@AnimatedContent
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
              )
            }
          }
        }
      }
    }
  }
}


class GridItem<T>(
  val key: T,
  val sizeState: MutableState<IntSize>,
)

private val debugColors =
  mutableListOf(Color.Cyan, Color.Red, Color.Green, Color.Yellow, Color.Blue, Color.Magenta)

private val calcCacheMap = mutableMapOf<String, CalcCache>()

data class CalcCache(var current: List<IntOffset>, var pre: List<IntOffset>?) {
  fun getResult(items: List<IntSize>) =
    if (pre == current && current.size == items.size) current else null
}

val debugCompose = Debugger("compose")

/**
 * 瀑布流计算器
 * 使用 LazyVerticalStaggeredGrid 进行计算，计算完成后(布局稳定之后)再自动进行隐藏
 * 使用时，建议进行缩放后再进行计算
 */
@Composable
fun waterfallCalc(
  columns: Int,
  items: List<IntSize>,
): List<IntOffset>? {
  val state = rememberLazyStaggeredGridState()
  val density = LocalDensity.current.density
  val key = "$columns/${items.joinToString(",") { "${it.width}x${it.height}" }}"
  val cache = calcCacheMap[key]
  return cache?.getResult(items) ?: run {
    Box(Modifier.height(0.dp)) {
      LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = Modifier.width(columns.dp)
          .requiredHeight(items.map { it.height }.reduce { acc, it -> acc + it }.dp)
          // 透明，不现实
          .alpha(
            when {
              debugCompose.isEnable -> 1f
              else -> 0f
            }
          ),
        state = state,
      ) {
        itemsIndexed(items) { index, item ->
          Box(
            Modifier.requiredSize(item.width.dp, item.height.dp)
              .background(debugColors[index % debugColors.size])
          )
        }
      }
    }
    val result = state.layoutInfo.visibleItemsInfo.sortedBy { it.index }.map { gridItemInfo ->
      gridItemInfo.offset / density
    }
    when (cache) {
      null -> CalcCache(result, null).also {
        calcCacheMap[key] = it
      }

      else -> {
        cache.pre = cache.current
        cache.current = result
        cache
      }
    }.getResult(items)
  }
}

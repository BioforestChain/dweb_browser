package org.dweb_browser.browser.web.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.div
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.helper.getCompletedOrNull
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.sys.window.core.LocalWindowController
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

enum class PreviewPanelVisibleState(val isVisible: Boolean) {
  DisplayGrid(true), Close(false), FastClose(false)/*不做动画，直接隐藏*/;
}

class BrowserPreviewPanel(val viewModel: BrowserViewModel) {
  val aniProgress = Animatable(0f)
  private val aniFastInSpec = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
  private val aniFastOutSpec = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
  private val aniInSpec1 = tween<Float>(3000, easing = EaseInQuad)
  private val aniOutSpec1 = tween<Float>(3000, easing = EaseOutQuad)
  private val aniInSpec = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
  private val aniOutSpec = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessLow)

  var previewPanelVisible by mutableStateOf(PreviewPanelVisibleState.Close)
  var previewReady by mutableStateOf(false)

  /**
   * 是否显示 Preview
   */
  val showPreview get() = previewPanelVisible == PreviewPanelVisibleState.DisplayGrid

  /**
   * Preview 是否不显示，同时 也不在 收起显示的动画中
   */
  val isPreviewInvisible get() = !showPreview && aniProgress.value == 0f
  val isPreviewVisible get() = !isPreviewInvisible
  fun toggleShowPreviewUI(state: PreviewPanelVisibleState) {
    previewPanelVisible = state
  }

  var withoutAnimationOnFocus by mutableStateOf(false)

  /**
   * 隐藏BrowserPreview，并且将PageState滚动时不适用动画效果
   */
  fun hideBrowserPreviewWithoutAnimation() {
    withoutAnimationOnFocus = true
    toggleShowPreviewUI(PreviewPanelVisibleState.Close)
  }

  /**
   * 显示多视图窗口
   */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  fun Render(modifier: Modifier = Modifier): Boolean {
    val viewModel = LocalBrowserViewModel.current
    // 直到动画完成
    if (isPreviewInvisible) {
      return false
    }
    val uiScope = rememberCoroutineScope()
    LocalWindowController.current.navigation.GoBackHandler {
      toggleShowPreviewUI(PreviewPanelVisibleState.Close)
    }

    val focusPagePreviewBoundsDeferred = remember {
      CompletableDeferred<Rect>().also {
        previewReady = false
        it.invokeOnCompletion { previewReady = true }
      }
    }

    /// 绑定 aniProgress
    LaunchedEffect(previewPanelVisible) {
      when (previewPanelVisible) {
        PreviewPanelVisibleState.DisplayGrid -> {
          focusPagePreviewBoundsDeferred.await()
          aniProgress.animateTo(1f, aniInSpec)
        }

        PreviewPanelVisibleState.Close -> {
          focusPagePreviewBoundsDeferred.cancel()
          aniProgress.animateTo(0f, aniOutSpec)
        }

        PreviewPanelVisibleState.FastClose -> {
          focusPagePreviewBoundsDeferred.cancel()
          aniProgress.snapTo(0f)
        }
      }
    }
    val p = aniProgress.value
    /// 预览图 + 底部工具栏
    Column(modifier) {
      val focusedPageIndex = viewModel.focusedPageIndex
      /// 预览图
      BoxWithConstraints(
        modifier = Modifier.weight(1f)
          .background(if (previewReady) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
      ) {
        val density = LocalDensity.current.density
        val pageSize = viewModel.pageSize

        val scrollState = rememberScrollState()
        focusPagePreviewBoundsDeferred.getCompletedOrNull()?.also { rect ->
          LaunchedEffect(rect) {
            scrollState.scrollTo(((rect.top - 16) * density).fastRoundToInt())
          }
        }

        fun calcCellSize(total: Float, count: Int, padding: Float = 16f, spacing: Float = 16f) =
          (total - padding - padding - spacing * (count - 1)) / count

        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val itemModifier = when {
          maxWidth.value < 240 -> Modifier.fillMaxWidth()
          maxWidth.value < 500 -> Modifier.width(calcCellSize(containerWidth.value, 2).dp)
          maxWidth.value < 800 -> Modifier.width(calcCellSize(containerWidth.value, 3).dp)
          maxWidth.value < 1080 -> Modifier.width(calcCellSize(containerWidth.value, 4).dp)
          else -> Modifier.width(250.dp)
        }

        FlowRow(
          Modifier.fillMaxSize().verticalScroll(scrollState)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
          horizontalArrangement = when (IPureViewController.isDesktop) {
            true -> Arrangement.spacedBy(space = 16.dp, alignment = Alignment.Start)
            else -> Arrangement.SpaceBetween
          },
//          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
//          verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
          overflow = FlowRowOverflow.Visible,
        ) {
          for (pageIndex in 0..<pageSize) {
            val page = viewModel.getPage(pageIndex)
            val isFocusedPage = focusedPageIndex == pageIndex
            val cellClosable = pageSize > 1 || page !is BrowserHomePage

            key(page.hashCode()) {
              PagePreviewCell(
                pageIndex = pageIndex,
                page = page,
                modifier = itemModifier.wrapContentHeight().padding(bottom = 16.dp)
                  .zIndex(if (isFocusedPage) 3f else 1f),
                closable = cellClosable,
                focus = isFocusedPage,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                focusPagePreviewBoundsDeferred = focusPagePreviewBoundsDeferred,
              )
            }
          }
        }
      }

      /// 底部工具栏
      PreviewBottomBar(
        uiScope = uiScope,
        viewModel = viewModel,
        focusedPageIndex = focusedPageIndex,
        modifier = Modifier.alpha(alpha = p).background(MaterialTheme.colorScheme.surface)
      )
    }
    return true
  }

  @Composable
  private fun PagePreviewCell(
    pageIndex: Int,
    page: BrowserPage,
    modifier: Modifier,
    closable: Boolean,
    focus: Boolean,
    containerWidth: Dp,
    containerHeight: Dp,
    focusPagePreviewBoundsDeferred: CompletableDeferred<Rect>,
  ) {
    val viewModel = LocalBrowserViewModel.current
    val scope = viewModel.browserNMM.getRuntimeScope()
    val uiScope = rememberCoroutineScope()
    val p = aniProgress.value

    /// 预览图和标题 + 关闭按钮
    Box(modifier) {
      /// 预览图和标题
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        /// 预览图
        val defaultAspectRatio = containerWidth.value / containerHeight.value
        var imageModifier = Modifier.fillMaxWidth()
          // 为了过渡动画更加平滑，这里禁用涟漪效果
          .clickableWithNoEffect {
            uiScope.launch {
              viewModel.focusPageUI(page)
              hideBrowserPreviewWithoutAnimation()
            }
          }.hoverCursor()
        if (focus) {
          if (p != 1f) {
            var rect by remember { mutableStateOf(Rect.Zero) }
            val density = LocalDensity.current.density
            imageModifier = imageModifier.onGloballyPositioned { coordinates ->
              /**
               * 这里与compose层级有很严格的依赖关系，如果不知道要使用几层parentLayoutCoordinates，可以开启下面代码进行打印调试
               * ```kotlin
               * var i = 1
               * var parent = coordinates.parentLayoutCoordinates
               * while (parent != null) {
               *   val position = parent.localPositionOf(coordinates)
               *   println("QAQ $i.position=$position")
               *   parent = parent.parentLayoutCoordinates
               *   i += 1
               * }
               * ```
               * 虽然理论上其它的更加标准的写法方案，但目前来说这种写法是最简单的有效的。除非有一种基于标记的方案：
               * ```kotlin
               * Modifier.markLayoutCoordinates("some-parent")
               *
               * coordinates.parentLayoutCoordinates("some-parent")?.localPositionOf(coordinates)
               * ```
               */
              coordinates.parentLayoutCoordinates?.parentLayoutCoordinates?.parentLayoutCoordinates?.parentLayoutCoordinates?.localPositionOf(
                coordinates
              )?.also { position ->
                focusPagePreviewBoundsDeferred.complete(Rect(
                  position / density, coordinates.size.div(density)
                ).also {
                  rect = it
                })
              }
            }
            when (focusPagePreviewBoundsDeferred.getCompletedOrNull()) {
              null -> {
                imageModifier = imageModifier.alpha(0f)
              }

              else -> {
                imageModifier = imageModifier.graphicsLayer {
                  val startX = -rect.left * density
                  val startY = -rect.top * density
                  val endX = 0
                  val endY = 0
                  val startWidth = containerWidth.value
                  val endWidth = rect.width
                  val startHeight = containerHeight.value
                  val endHeight = rect.height
                  translationX = startX + (endX - startX) * p
                  translationY = startY + (endY - startY) * p
                  scaleX = (startWidth + (endWidth - startWidth) * p) / endWidth
                  scaleY = (startHeight + (endHeight - startHeight) * p) / endHeight
                  transformOrigin = TransformOrigin(0f, 0f)
                }.zIndex(2f)
              }
            }
          }
        } else {
          imageModifier = imageModifier.alpha(alpha = p)
        }

        BoxWithConstraints(
          imageModifier,
          contentAlignment = Alignment.TopCenter,
        ) {
          /// 预览图的动态阴影和圆角
          val innerP = if (focus) p else 1f
          val shadowColor = LocalContentColor.current.copy(alpha = innerP)
          val shape = SquircleShape(
            lerp(0.dp, 20.dp, innerP), lerp(CornerSmoothing.None, CornerSmoothing.Small, innerP)
          )

          val innerModifier = Modifier.fillMaxSize().aspectRatio(defaultAspectRatio).shadow(
            elevation = if (focus) lerp(0.dp, 4.dp, innerP) else 1.dp,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor,
          ).clip(shape).background(MaterialTheme.colorScheme.surface)

          page.PreviewRender(containerWidth = maxWidth, modifier = innerModifier)
        }

        /// 图标和标题
        Row(
          modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(top = 4.dp)
            .align(Alignment.CenterHorizontally).alpha(alpha = p),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = CenterVertically
        ) {
          Box(
            Modifier.padding(end = 4.dp).requiredSize(16.dp).clip(CircleShape)
              .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              "${pageIndex + 1}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.scale(0.62f)
            )
          }

          page.icon?.let { iconPainter ->
            Image(
              painter = iconPainter,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              colorFilter = page.iconColorFilter
            )
            Spacer(modifier = Modifier.width(2.dp))
          }
          Text(
            text = page.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
          )
        }
      }
      /// 关闭按钮
      if (closable) {
        val iconAni = remember { Animatable(0f) }
        LaunchedEffect(showPreview, p == 1f) {
          when {
            (showPreview && p == 1f) -> iconAni.animateTo(1f, aniFastInSpec)
            else -> iconAni.animateTo(0f, aniFastOutSpec)
          }
        }
        FilledTonalIconButton(
          onClick = {
            scope.launch { viewModel.closePageUI(page) }
          },
          modifier = Modifier.align(Alignment.TopEnd).alpha(iconAni.value).scale(iconAni.value)
            .hoverCursor().zIndex(3f),
        ) {
          Icon(Icons.Rounded.Close, "Close Page")
        }
      }
    }
  }

  @Composable
  private fun PreviewBottomBar(
    uiScope: CoroutineScope,
    viewModel: BrowserViewModel,
    focusedPageIndex: Int,
    modifier: Modifier,
  ) {
    Row(
      modifier = modifier.fillMaxWidth().requiredHeight(dimenBottomHeight)
        // 因为本质上和 tabsbar 层叠在一起渲染，所以这里拦截掉所有事件
        .pointerInput(Unit) { awaitPointerEventScope { } }, verticalAlignment = CenterVertically
    ) {
      // 添加新页面按钮
      IconButton(onClick = {
        uiScope.launch {
          toggleShowPreviewUI(PreviewPanelVisibleState.FastClose)
          viewModel.addNewPageUI {
            addIndex = focusedPageIndex + 1
            focusPage = true
          }
        }
      }) {
        Icon(
          imageVector = Icons.Default.Add, // ImageVector.vectorResource(id = R.drawable.ic_main_add),
          contentDescription = "Add New Page",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      val content = BrowserI18nResource.browser_multi_count()
      Text(
        text = "${viewModel.pageSize} $content",
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      // 关闭 PreviewPanel 按钮
      TextButton(onClick = {
        toggleShowPreviewUI(PreviewPanelVisibleState.Close)
      }) {
        Text(
          text = BrowserI18nResource.browser_multi_done(),
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}


expect fun calculateGridsCell(pageSize: Int, maxWidth: Dp, maxHeight: Dp): Triple<Int, Dp, Dp>

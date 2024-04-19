package org.dweb_browser.browser.web.ui

import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.browser.web.model.page.BrowserPage
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.platform.theme.DimenBottomBarHeight
import org.dweb_browser.sys.window.render.LocalWindowController
import kotlin.math.max

/**
 * 显示多视图窗口
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
internal fun BrowserPreviewPanel(modifier: Modifier = Modifier) {
  val viewModel = LocalBrowserViewModel.current
  // 使用动画替代
  if (viewModel.isPreviewInvisible) {
    // 停止渲染后，销毁 RenderReady  这个状态值
    viewModel.previewPanelAnimationReady.clear()
    return
  }
  val uiScope = rememberCoroutineScope()
  LocalWindowController.current.GoBackHandler {
    viewModel.toggleShowPreviewUI(false)
  }

  Column(
    modifier = modifier.fillMaxSize().background(
      MaterialTheme.colorScheme.surface.copy(
        alpha = if (viewModel.previewPanelAnimationReady.isNotEmpty()) 1f else 0f
      )
    )
  ) {
    val focusedPageIndex = viewModel.focusedPageIndex
    val lazyGridState = rememberLazyGridState(max(focusedPageIndex, 0))
    val panelTransition = rememberTransition(viewModel.previewPanelVisibleState)
    BoxWithConstraints(modifier = Modifier.weight(1f)) {
      val pageSize = viewModel.pageSize
      val onlyOne = pageSize <= 1
      val cellWidth = remember(onlyOne, maxWidth) {
        when (onlyOne) {
          true -> maxWidth * 0.618f
          else -> maxWidth * 0.8f / 2
        }
      }
      val cellHeight = remember(cellWidth) {
        cellWidth * 1.618f
      }
      LazyVerticalGrid(
        columns = GridCells.Fixed(if (onlyOne) 1 else 2),
        modifier = Modifier.fillMaxSize(),
        state = lazyGridState,
        contentPadding = PaddingValues(vertical = 20.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        items(pageSize) { pageIndex ->
          val page = viewModel.getPage(pageIndex)
          val isFocusedPage = focusedPageIndex == pageIndex
          val cellModifier =
            Modifier.requiredSize(cellWidth, cellHeight + 16.dp).padding(bottom = 16.dp)
          val cellClosable = !(pageSize == 1 && page is BrowserHomePage)
          if (isFocusedPage || viewModel.previewPanelAnimationReady.contains(pageIndex)) {
            val animationInitState = when {
              // 进入动画 的起点：只有聚焦页面是最大化的
              viewModel.showPreview -> when {
                isFocusedPage -> PagePreviewState.Max
                else -> PagePreviewState.Min
              }
              // 离开动画 的起点：所有页面都是从最小化开始的
              else -> PagePreviewState.Min
            }
            val animationTargetState = when {
              // 进入动画 的终点：所有都最小化
              viewModel.showPreview -> PagePreviewState.Min
              // 离开动画 的终点：只有聚焦页面最大化
              else -> when {
                isFocusedPage -> PagePreviewState.Max
                else -> PagePreviewState.Min
              }
            }
            PagePreviewCellWithAnimation(
              panelTransition = panelTransition,
              page = page,
              pageIndex = pageIndex,
              parentWidth = maxWidth,
              parentHeight = maxHeight,
              contentWidth = cellWidth,
              contentHeight = cellHeight,
              animationInitState = animationInitState,
              animationTargetState = animationTargetState,
              modifier = cellModifier.let { if (isFocusedPage) it.zIndex(pageSize.toFloat()) else it },
              closable = cellClosable,
              focus = isFocusedPage,
            )
          } else {
            PagePreviewCell(
              page = page,
              modifier = cellModifier,
              closable = cellClosable,
              focus = false,
            )
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().height(dimenBottomHeight)
        .background(MaterialTheme.colorScheme.surface), verticalAlignment = CenterVertically
    ) {
      IconButton(onClick = {
        uiScope.launch {
          viewModel.addNewPageUI {
            addIndex = focusedPageIndex + 1
            focusPage = true
          }
          viewModel.toggleShowPreviewUI(false)
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
      TextButton(onClick = { viewModel.toggleShowPreviewUI(false) }) {
        Text(
          text = BrowserI18nResource.browser_multi_done(),
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

private enum class PagePreviewState(val isMinimal: Boolean) {
  Min(true), Max(false), ;
}

/**
 * 基于左上角的动画
 *
 * @param animationTargetState 制定的目标动画状态
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun PagePreviewCellWithAnimation(
  panelTransition: Transition<BrowserViewModel.PreviewPanelVisibleState>,
  page: BrowserPage,
  pageIndex: Int,
  parentWidth: Dp,
  parentHeight: Dp,
  contentWidth: Dp,
  contentHeight: Dp,
  animationInitState: PagePreviewState,
  animationTargetState: PagePreviewState,
  modifier: Modifier,
  closable: Boolean,
  focus: Boolean,
) {
  val viewModel = LocalBrowserViewModel.current
  // 这里不 remember animationInitState，动画要有延续性
  val pageVisibleState = remember { MutableTransitionState(animationInitState) }

  /**
   * 使用上面创建的 转换状态 创建一个 转换器
   */
  val pageTransition = rememberTransition(pageVisibleState)

  LaunchedEffect(animationTargetState) {
    pageVisibleState.targetState = animationTargetState
  }

  val ready = viewModel.previewPanelAnimationReady.contains(pageIndex)
  val density = LocalDensity.current.density
  val aniOrigin = remember { TransformOrigin(0f, 0f) }
  var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  // 左上角的动画起点
  var fromX by remember { mutableFloatStateOf(0f) }
  var fromY by remember { mutableFloatStateOf(0f) }
  val toX = 0f
  val toY = 0f

  // 元素大小，这里只改变高度
  val fromHeight = remember(parentWidth, parentHeight, contentWidth) {
    (parentHeight.value / parentWidth.value) * contentWidth.value
  }
  var toHeight by remember { mutableFloatStateOf(contentHeight.value) }
  // 元素缩放，用于同时改变宽高
  val fromScale = remember(parentWidth, contentWidth) {
    parentWidth.value / contentWidth.value
  }
  val toScale = 1f

  /**
   * 配置动画
   * 从 Max 到 Min，属于 enter 动画
   */
  val aniSpec = remember(pageVisibleState.targetState.isMinimal) {
    if (pageVisibleState.targetState.isMinimal) enterAnimationSpec<Float>()
    else exitAnimationSpec<Float>()
  }
  // 将动画强制赋值给 panelTransition，这样可以使得 panel 也能假装有动画，isIdle==false，确保动画完成播放。但是这里没有考虑时间，也不需要考虑时间
  key(aniSpec) {
    panelTransition.animateFloat(
      transitionSpec = { aniSpec }, label = ""
    ) { if (it.isVisible) 1f else 0f }
  }

  val aniX by key(fromX, aniSpec) {
    pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
      if (it.isMinimal) toX else fromX
    }
  }
  val aniY by key(fromY, aniSpec) {
    pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
      if (it.isMinimal) toY else fromY
    }
  }
  val aniHeight by key(toHeight, aniSpec) {
    pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
      if (it.isMinimal) toHeight else fromHeight
    }
  }
  val aniScale by key(aniSpec) {
    pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
      if (it.isMinimal) toScale else fromScale
    }
  }
  val aniElevation by key(focus) {
    pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
      if (it.isMinimal) (if (focus) 4f else 1f) else 0f
    }
  }
  val aniShape by pageTransition.animateFloat(transitionSpec = { aniSpec }, label = "") {
    if (it.isMinimal) 16f else 0f
  }

  PagePreviewCell(
    page = page, modifier = modifier.onGloballyPositioned {
      parentCoordinates = it.parentLayoutCoordinates
    }, closable = closable, focus = focus,
    //
    previewModifier = Modifier.onGloballyPositioned {
      parentCoordinates?.also { parent ->
        it.localPositionOf(parent, Offset.Zero).apply {
          fromX = x
          fromY = y
        }
        toHeight = it.size.height / density
        if (!ready) {
          viewModel.previewPanelAnimationReady.add(pageIndex)
        }
      }
    }.graphicsLayer(
      scaleX = aniScale,
      scaleY = aniScale,
      translationX = aniX,
      translationY = aniY,
      transformOrigin = aniOrigin,
      alpha = if (ready) 1f else 0f
    ).requiredSize(contentWidth, aniHeight.dp).shadow(
      elevation = aniElevation.dp,
      shape = RoundedCornerShape(aniShape.dp),
      ambientColor = LocalContentColor.current,
      spotColor = LocalContentColor.current,
    ).clip(RoundedCornerShape(aniShape.dp)).background(MaterialTheme.colorScheme.onSurface)
  )
}

@Composable
private fun PagePreviewCell(
  page: BrowserPage,
  modifier: Modifier,
  closable: Boolean,
  focus: Boolean,
  previewModifier: Modifier = Modifier.fillMaxSize().shadow(
    elevation = if (focus) 4.dp else 1.dp, shape = RoundedCornerShape(16.dp),
    ambientColor = LocalContentColor.current,
    spotColor = LocalContentColor.current,
  ).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.onSurface)
) {
  val viewModel = LocalBrowserViewModel.current
  val scope = viewModel.browserNMM.ioAsyncScope
  val uiScope = rememberCoroutineScope()

  Box(modifier) {
    if (closable && viewModel.showPreview) {
      IconButton(
        onClick = {
          scope.launch { viewModel.closePageUI(page) }
        },
        modifier = Modifier.align(Alignment.TopEnd).zIndex(3f),
        colors = IconButtonDefaults.iconButtonColors()
          .copy(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Close Page",
        )
      }
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      val pageTitle = page.title
      val pageIcon = page.icon
      val pageIconColorFilter = page.iconColorFilter
      val pagePreview = page.previewContent
      Box(Modifier.weight(1f).fillMaxWidth().zIndex(2f)) {
        BoxWithConstraints(
          previewModifier.background(MaterialTheme.colorScheme.surface)
            // 为了过渡动画更加平滑，这里禁用涟漪效果
            .clickableWithNoEffect {
              uiScope.launch {
                viewModel.focusPageUI(page)
                viewModel.toggleShowPreviewUI(false)
              }
            },
          contentAlignment = if (pagePreview != null) Alignment.TopCenter else Alignment.Center,
        ) {
          if (pagePreview != null) {
            Image(
              painter = pagePreview,
              contentDescription = pageTitle,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.FillWidth,
              alignment = Alignment.Center,
            )
          } else if (pageIcon != null) {
            Image(
              painter = pageIcon,
              contentDescription = pageTitle,
              colorFilter = pageIconColorFilter,
              modifier = Modifier.size(maxWidth / 3),
            )
          } else {
            Icon(
              imageVector = Icons.Default.BrokenImage,
              contentDescription = pageTitle,
              modifier = Modifier.size(maxWidth / 3),
              tint = LocalContentColor.current.copy(alpha = 0.5f)
            )
          }
        }
      }
      Row(
        modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(top = 4.dp)
          .align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = CenterVertically
      ) {
        pageIcon?.let { iconPainter ->
          Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = pageIconColorFilter
          )
          Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text = pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
      }
    }
  }
}
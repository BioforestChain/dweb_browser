package org.dweb_browser.browser.jmm.render

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest

/**
 * 图片预览图
 * @param visibleState 本来想使用 MutableTransitionState，但是后面发现进入和退出的聚焦点会动态变化，这样子就会导致这个组件每次都会重组，所以也可以直接改为 MutableState
 * @param select 当前查看的图片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImagePreview(
  jmmAppInstallManifest: JmmAppInstallManifest,
  previewState: PreviewState,
) {
  AnimatedVisibility(
    visibleState = previewState.showPreview,
    enter = scaleIn(
      initialScale = 0.3f,
      transformOrigin = TransformOrigin(previewState.offset.value.x, previewState.offset.value.y)
    ) + fadeIn(),
    exit = scaleOut(
      targetScale = 0.3f,
      transformOrigin = TransformOrigin(previewState.offset.value.x, previewState.offset.value.y)
    ) + fadeOut(),
  ) {
    BackHandler { previewState.showPreview.targetState = false }
    val pagerState = rememberPagerState(initialPage = previewState.selectIndex.value,
      initialPageOffsetFraction = 0f,
      pageCount = { jmmAppInstallManifest.images.size })
    val imageList = jmmAppInstallManifest.images

    LaunchedEffect(previewState) { // 为了滑动图片后，刷新后端的图片中心点位置
      snapshotFlow { pagerState.currentPage }.collect { pager ->
        previewState.offset.value = measureCenterOffset(pager, previewState)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      HorizontalPager(modifier = Modifier.fillMaxSize(),
        state = pagerState,
        pageSpacing = 0.dp,
        userScrollEnabled = true,
        reverseLayout = false,
        contentPadding = PaddingValues(0.dp),
        beyondBoundsPageCount = 0,
        pageContent = { index ->
          AsyncImage(model = imageList[index],
            contentDescription = "Picture",
            alignment = Alignment.Center,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
              .fillMaxSize()
              .clickableWithNoEffect { previewState.showPreview.targetState = false })
        })
      Row(
        Modifier
          .height(50.dp)
          .fillMaxWidth()
          .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.Center
      ) {
        repeat(imageList.size) { iteration ->
          val color = if (pagerState.currentPage == iteration) Color.LightGray else Color.DarkGray
          Box(
            modifier = Modifier
              .padding(2.dp)
              .clip(CircleShape)
              .background(color)
              .size(8.dp)
          )
        }
      }
    }
  }
}
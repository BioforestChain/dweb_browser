package org.dweb_browser.browser.web.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.web.debugBrowser

class BrowserPagerStates(val viewModel: BrowserViewModel) {
  private inner class InnerPagerState : PagerState(0, 0f) {
    override val pageCount: Int
      get() = viewModel.pageSize
  }

  val focusedPageIndexState = mutableStateOf(-1)
  var focusedPageIndex
    get() = focusedPageIndexState.value
    set(value) {
      focusedPageIndexState.value = value
      if (useSearchBar) {
        tabsBarPager.requestScrollToPage(value)
      }
    }

  /**
   * 用于表示展示内容
   */
  val contentLazyState = LazyListState()

  @Composable
  fun ContentPageEffect() {
    var isFirstView by remember { mutableStateOf(true) }
    if (!useSearchBar) {
      val viewPortWidth = contentLazyState.layoutInfo.viewportSize.width
      LaunchedEffect(focusedPageIndex, viewPortWidth) {
        launch(start = CoroutineStart.UNDISPATCHED) {
          debugBrowser("ContentPageEffect") {
            "contentLazyState.animateScrollToItem($focusedPageIndex) isFirstView=$isFirstView"
          }
          if (isFirstView) {
            contentLazyState.scrollToItem(focusedPageIndex)
          } else {
            contentLazyState.animateScrollToItem(focusedPageIndex)
          }
        }
        launch(start = CoroutineStart.UNDISPATCHED) {
          debugBrowser("ContentPageEffect") {
            "tabsBarLazyState.animateScrollToItem($focusedPageIndex)"
          }
          tabsBarLazyState.animateScrollToItem(focusedPageIndex)
        }
      }
    }
    LaunchedEffect(Unit) {
      delay(200)
      isFirstView = false
      println("QAQ isFirstView done")
    }
  }

  val tabsBarLazyState = LazyListState()

  /**
   * 用于表示下面搜索框等内容
   */
  val tabsBarPager: PagerState = InnerPagerState()
  private var useSearchBar by mutableStateOf(false)

  @Composable
  fun SearchBarEffect() {
    DisposableEffect(this) {
      tabsBarPager.requestScrollToPage(focusedPageIndex)
      useSearchBar = true
      onDispose {
        useSearchBar = false
      }
    }
    val viewPortWidth = contentLazyState.layoutInfo.viewportSize.width
    LaunchedEffect(
      tabsBarPager.currentPage,
      tabsBarPager.currentPageOffsetFraction,
      viewPortWidth
    ) {
      var currentPage = tabsBarPager.currentPage
      var currentPageOffsetFraction = tabsBarPager.currentPageOffsetFraction

      /**
       * 矫正数值
       *
       * 由于HorizontalPager的有效区间值是 -0.5f~0.5f ,荣耀手机在这块兼容出问题了，导致出现了不在区间的值，
       * 所以在这边强制限制值必须在 -0.5f~0.5f 之间
       */
      while (true) {
        if (currentPageOffsetFraction > 0.5f) {
          currentPage += 1
          currentPageOffsetFraction = 1 - currentPageOffsetFraction
        } else if (currentPageOffsetFraction < -0.5f) {
          currentPage -= 1
          currentPageOffsetFraction = -1 - currentPageOffsetFraction
        } else {
          break
        }
      }
      // 执行滚动
      val scrollOffset = viewPortWidth * currentPageOffsetFraction
      contentLazyState.scrollToItem(currentPage, scrollOffset.fastRoundToInt())
    }
  }
}

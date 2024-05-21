package org.dweb_browser.browser.web.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.dweb_browser.browser.web.ui.enterAnimationSpec

@OptIn(ExperimentalFoundationApi::class)
class BrowserPagerStates(val viewModel: BrowserViewModel) {
  private inner class InnerPagerState : PagerState(0, 0f) {
    override val pageCount: Int
      get() = viewModel.pageSize
  }

  /**
   * 用于表示展示内容
   */
  val contentPage: PagerState = InnerPagerState()

  /**
   * 用于表示下面搜索框等内容
   */
  val searchBar: PagerState = InnerPagerState()

  @Composable
  fun BindingEffect() {
    val viewModel = LocalBrowserViewModel.current
    val searchBarPager = searchBar
    val contentPagePager = contentPage

    if (viewModel.isFillPageSize) {
      /// searchBarPager => contentPagePager
      LaunchedEffect(searchBarPager.currentPage, searchBarPager.currentPageOffsetFraction) {
        var currentPage = searchBarPager.currentPage
        var currentPageOffsetFraction = searchBarPager.currentPageOffsetFraction

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
        contentPagePager.scrollToPage(currentPage, currentPageOffsetFraction)
      }

      // focusedPage => searchPagePager
      LaunchedEffect(viewModel.focusedPageIndex) {
        val pageIndex = viewModel.focusedPageIndex
        if (!searchBarPager.isScrollInProgress) {
          searchBarPager.animateScrollToPage(pageIndex, animationSpec = enterAnimationSpec())
        }
      }

      /// contentPagePager => focusedPage
      LaunchedEffect(contentPagePager.currentPage, contentPagePager.isScrollInProgress) {
        if (!contentPagePager.isScrollInProgress) {
          viewModel.focusPageUI(contentPagePager.currentPage)
        }
      }
    } else {
      /// searchBarPager => contentPagePager
      // 目前桌面端的 PageSize使用 Fixed，所以这边不关心 currentPageOffsetFraction 值
      LaunchedEffect(
        searchBarPager.currentPage,
        searchBarPager.isScrollInProgress,
        searchBarPager.currentPageOffsetFraction
      ) {
        // 考虑到如果聚焦到最后一个page时，currentPage有偏移量的话，不进行聚焦
        if (!searchBarPager.isScrollInProgress && searchBarPager.currentPageOffsetFraction == 0f) {
          viewModel.focusPageUI(searchBarPager.currentPage)
        }
      }

      // focusedPage => searchPagePager
      LaunchedEffect(viewModel.focusedPageIndex, contentPagePager.isScrollInProgress) {
        val pageIndex = viewModel.focusedPageIndex
        if (!contentPagePager.isScrollInProgress) {
          searchBarPager.scrollToPage(pageIndex)
          contentPagePager.animateScrollToPage(pageIndex)
        }
      }
    }
  }
}

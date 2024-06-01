package org.dweb_browser.browser.web.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import org.dweb_browser.sys.window.core.WindowContentRenderScope

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

  private var lastWindowRenderScope = WindowContentRenderScope(0f, 0f, 0f)
  private var isResizeWin by mutableStateOf(false) // 用于判断是否是窗口大小变化，如果是的话，不响应searchBar的监听

  @Composable
  fun BindingEffect(windowRenderScope: WindowContentRenderScope) {
    val viewModel = LocalBrowserViewModel.current
    val searchBarPager = searchBar
    val contentPagePager = contentPage

    if (viewModel.isFillPageSize) {
      // searchBarPager => contentPagePager
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
      LaunchedEffect(Unit) {
        snapshotFlow {
          Pair(viewModel.focusedPageIndex, searchBarPager.isScrollInProgress)
        }.collect { (pageIndex, isScrollInProgress) ->
          if (!isScrollInProgress) {
            // 由于如果是通过BrowserPreview点击后聚焦的，那么滚动时候需要直接滚动，不要动画滚动。
            if (viewModel.withoutAnimationOnFocus) {
              searchBarPager.scrollToPage(pageIndex)
            } else {
              searchBarPager.animateScrollToPage(pageIndex)
            }
            viewModel.withoutAnimationOnFocus = false
          }
        }
      }

      // contentPagePager => focusedPage
      LaunchedEffect(Unit) {
        snapshotFlow {
          Pair(contentPage.currentPage, contentPage.isScrollInProgress)
        }.collect { (currentPage, isScrollInProgress) ->
          if (!isScrollInProgress) {
            viewModel.focusPageUI(currentPage)
          }
        }
      }
    } else {
      // 为了确认下当前窗口是否有改变大小
      if (lastWindowRenderScope.width != windowRenderScope.width ||
        lastWindowRenderScope.height != windowRenderScope.height
      ) {
        isResizeWin = true
        lastWindowRenderScope = windowRenderScope
      }

      // focusedPage => searchPagePager
      LaunchedEffect(Unit) {
        snapshotFlow {
          Pair(viewModel.focusedPageIndex, contentPagePager.isScrollInProgress)
        }.collect { (focusedPageIndex, isScrollInProgress) ->
          if (!isScrollInProgress) {
            contentPagePager.animateScrollToPage(focusedPageIndex)
          }
        }
      }

      LaunchedEffect(Unit) {
        snapshotFlow {
          Pair(contentPage.currentPage, contentPage.isScrollInProgress)
        }.collect { (currentPage, isScrollInProgress) ->
          if (!isScrollInProgress) {
            searchBarPager.scrollToPage(currentPage)
          }
        }
      }

      // contentPage => focusedUI
      LaunchedEffect(Unit) {
        snapshotFlow {
          Triple(
            searchBar.currentPage, searchBar.isScrollInProgress, searchBar.currentPageOffsetFraction
          )
        }.collect { (currentPage, isScrollInProgress, currentPageOffsetFraction) ->
          // 考虑到如果聚焦到最后一个page时，currentPage有偏移量的话，不进行聚焦
          if (!isScrollInProgress && currentPageOffsetFraction == 0f && !isResizeWin) {
            viewModel.focusPageUI(currentPage)
          }
          isResizeWin = false
        }
      }

      // isResizeWin -> searchBarPager
      LaunchedEffect(Unit) {
        snapshotFlow { isResizeWin }.collect { isResize ->
          if (isResize && !searchBarPager.isScrollInProgress) {
            searchBarPager.scrollToPage(viewModel.focusedPageIndex)
          }
        }
      }
    }
  }
}

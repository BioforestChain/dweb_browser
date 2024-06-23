package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmDetailController
import org.dweb_browser.browser.jmm.LocalJmmDetailController
import org.dweb_browser.browser.jmm.render.AppBottomHeight
import org.dweb_browser.browser.jmm.render.BottomDownloadButton
import org.dweb_browser.browser.jmm.render.CaptureListView
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.WebviewVersionWarningDialog
import org.dweb_browser.browser.jmm.render.app.AppIntroductionView
import org.dweb_browser.browser.jmm.render.app.NewVersionInfoView
import org.dweb_browser.browser.jmm.render.app.OtherInfoView
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitle
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState


enum class JmmDetailTabs(val i18n: SimpleI18nResource) {
  /** 参数信息 */
  Param(BrowserI18nResource.JMM.tab_param),

  /** 详情 */
  Detail(BrowserI18nResource.JMM.tab_detail),

  /** 介绍 */
  Intro(BrowserI18nResource.JMM.tab_intro),
  ;

  companion object {
    val ALL = entries.toList()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JmmDetailController.Render(modifier: Modifier, renderScope: WindowContentRenderScope) {
  val win = LocalWindowController.current

  win.navigation.GoBackHandler(enabled = CanCloseBottomSheet()) {
    closeBottomSheet()
  }

  LocalCompositionChain.current.Provider(LocalJmmDetailController provides this) {
    renderScope.WindowContentScaffoldWithTitle(
      modifier = modifier,
      topBarTitle = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          metadata.manifest.IconRender()
          Spacer(Modifier.size(16.dp))
          Text(metadata.manifest.name, maxLines = 2)
        }
      },
      content = {
        val uiScope = rememberCoroutineScope()
        Box(Modifier.fillMaxWidth().padding(it).run {
          if (CanCloseBottomSheet()) {
            val bottomSafePadding = win.watchedState { safePadding }.value.bottom
            padding(PaddingValues(bottom = bottomSafePadding.dp))
          } else this
        }) {
          val hasDetail = metadata.manifest.images.isNotEmpty()

          Column(
            Modifier.fillMaxWidth().padding(bottom = AppBottomHeight)
          ) {
            val allTabs = JmmDetailTabs.ALL.filter { tab ->
              when (tab) {
                JmmDetailTabs.Detail -> hasDetail
                else -> true
              }
            }
            val lazyListState = rememberLazyListState()
            var indexByTabClick by remember { mutableStateOf(-1) }
            var selectedTabIndex by remember { mutableStateOf(0) }
            remember(lazyListState.isScrollInProgress, lazyListState.firstVisibleItemIndex) {
              /**
               * 目标tabIndex
               */
              val toTabIndex =
                when (!lazyListState.isScrollInProgress && !lazyListState.canScrollForward && lazyListState.canScrollBackward) {
                  // 如果不能再向下滚动了，那么设置成 lastIndex
                  true -> allTabs.size - 1
                  else -> lazyListState.firstVisibleItemIndex
                }

              if (indexByTabClick == -1) {
                selectedTabIndex = toTabIndex
              } else {
                /// 如果这次滚动是tabClick触发的，那么消费掉这次判断
                if (!lazyListState.isScrollInProgress) {
                  indexByTabClick = -1
                }
              }
            }
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
              allTabs.forEachIndexed { tabIndex, tab ->
                Tab(
                  selected = selectedTabIndex == tabIndex,
                  onClick = {
                    selectedTabIndex = tabIndex
                    indexByTabClick = tabIndex
                    uiScope.launch(start = CoroutineStart.UNDISPATCHED) {
                      lazyListState.animateScrollToItem(tabIndex)
                    }
                  },
                ) {
                  Text(tab.i18n(), modifier = Modifier.padding(10.dp))
                }
              }
            }

            LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
              for (tab in allTabs) {
                when (tab) {
                  JmmDetailTabs.Detail -> item(key = tab) {
                    println("QAQ CaptureListView")
                    CaptureListView(metadata.manifest)
                  }

                  JmmDetailTabs.Intro -> item(key = tab) {
                    AppIntroductionView(metadata.manifest)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
                    NewVersionInfoView(metadata.manifest)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
                  }

                  JmmDetailTabs.Param -> item(key = tab) {
                    OtherInfoView(metadata.manifest)
                  }
                }
              }
            }
          }

          BottomDownloadButton()
          WebviewVersionWarningDialog()
        }
      },
    )
  }
}
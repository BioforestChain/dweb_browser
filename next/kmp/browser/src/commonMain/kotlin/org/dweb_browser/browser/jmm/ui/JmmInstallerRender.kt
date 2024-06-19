package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.browser.jmm.LocalJmmInstallerController
import org.dweb_browser.browser.jmm.render.AppBottomHeight
import org.dweb_browser.browser.jmm.render.BottomDownloadButton
import org.dweb_browser.browser.jmm.render.CaptureListView
import org.dweb_browser.browser.jmm.render.CustomerDivider
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.ImagePreview
import org.dweb_browser.browser.jmm.render.PreviewState
import org.dweb_browser.browser.jmm.render.WebviewVersionWarningDialog
import org.dweb_browser.browser.jmm.render.app.AppIntroductionView
import org.dweb_browser.browser.jmm.render.app.NewVersionInfoView
import org.dweb_browser.browser.jmm.render.app.OtherInfoView
import org.dweb_browser.browser.jmm.render.measureCenterOffset
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.compose.rememberScreenSize
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitle
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.watchedState


enum class JmmDetailTabs(val i18n: SimpleI18nResource) {
  /** 详情 */
  Detail(BrowserI18nResource.JMM.tab_detail),

  /** 介绍 */
  Intro(BrowserI18nResource.JMM.tab_intro),

  /** 参数信息 */
  Param(BrowserI18nResource.JMM.tab_param),
  ;

  companion object {
    val ALL = entries.toList()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JmmInstallerController.Render(modifier: Modifier, renderScope: WindowContentRenderScope) {
  val previewListState = rememberLazyListState()
  val screenSize = rememberScreenSize()
  val density = LocalDensity.current.density
  val statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)
  val previewState = remember {
    PreviewState(
      outsideLazy = previewListState,
      screenWidth = screenSize.screenWidth,
      screenHeight = screenSize.screenHeight,
      statusBarHeight = statusBarHeight,
      density = density
    )
  }

  val win = LocalWindowController.current

  win.navigation.GoBackHandler(enabled = CanCloseBottomSheet()) {
    closeBottomSheet()
  }
  win.navigation.GoBackHandler(enabled = previewState.showPreview.targetState) {
    previewState.showPreview.targetState = false
  }

  LocalCompositionChain.current.Provider(LocalJmmInstallerController provides this) {

    renderScope.WindowContentScaffoldWithTitle(
      modifier = modifier,
      topBarTitle = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          installMetadata.manifest.IconRender()
          Spacer(Modifier.size(16.dp))
          Text(installMetadata.manifest.name, maxLines = 2)
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
          val hasDetail = installMetadata.manifest.images.isNotEmpty()

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
                    CaptureListView(installMetadata.manifest) { index, imageLazyListState ->
                      previewState.selectIndex.value = index
                      previewState.imageLazy = imageLazyListState
                      previewState.offset.value = measureCenterOffset(index, previewState)
                      previewState.showPreview.targetState = true
                    }
                  }

                  JmmDetailTabs.Intro -> item(key = tab) {
                    AppIntroductionView(installMetadata.manifest)
                    CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
                    NewVersionInfoView(installMetadata.manifest)
                    CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
                  }

                  JmmDetailTabs.Param -> item(key = tab) {
                    OtherInfoView(installMetadata.manifest)
                  }
                }
              }
            }
          }

          BottomDownloadButton()
          ImagePreview(installMetadata.manifest, previewState)
          WebviewVersionWarningDialog()
        }
      },
    )
  }
}
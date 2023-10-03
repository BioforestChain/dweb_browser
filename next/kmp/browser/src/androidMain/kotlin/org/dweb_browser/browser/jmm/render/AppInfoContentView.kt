package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest

@Composable
internal fun AppInfoContentView(
  lazyListState: LazyListState,
  jmmAppInstallManifest: JmmAppInstallManifest,
  onSelectPic: (Int, LazyListState) -> Unit
) {
  LazyColumn(
    state = lazyListState,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      //.padding(top = TopBarHeight)
  ) {
    // 头部内容， HeadHeight 128.dp
    item { AppInfoHeadView(jmmAppInstallManifest) }
    // 应用信息， 88.dp
    /*item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(topStart = ShapeCorner, topEnd = ShapeCorner))
          .background(MaterialTheme.colorScheme.surface)
      ) {
        AppInfoLazyRow(jmmAppInstallManifest)
      }
    }*/
    // 上面padding 16.dp
    item {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        CaptureListView(jmmAppInstallManifest, onSelectPic)
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        AppIntroductionView(jmmAppInstallManifest)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        NewVersionInfoView(jmmAppInstallManifest)
        CustomerDivider(modifier = Modifier.padding(horizontal = HorizontalPadding))
        OtherInfoView(jmmAppInstallManifest)
        Spacer(modifier = Modifier.height(AppBottomHeight))
      }
    }
  }
}
package org.dweb_browser.browser.jmm.render.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.jmm.render.AppBottomHeight
import org.dweb_browser.browser.jmm.render.CaptureListView
import org.dweb_browser.browser.jmm.render.CustomerDivider
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.core.help.types.JmmAppInstallManifest

@Composable
internal fun JmmAppInstallManifest.Render(
  onSelectPic: (Int, LazyListState) -> Unit
) {
  val jmmAppInstallManifest = this
  LazyColumn(
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
package info.bagen.dwebbrowser.microService.browser.jmm.render.appinstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest

@Composable
internal fun AppInfoContentView(
  jmmAppInstallManifest: JmmAppInstallManifest,
  onSelectPic: (Int, LazyListState) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 头部内容
    AppInfoHeadView(jmmAppInstallManifest)
    // 应用信息
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = ShapeCorner, topEnd = ShapeCorner))
        .background(MaterialTheme.colorScheme.surface)
    ) {
      AppInfoLazyRow(jmmAppInstallManifest)
    }
    // 上面padding 16.dp
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      CustomerDivider()
      CaptureListView(jmmAppInstallManifest, onSelectPic)
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      CustomerDivider()
      AppIntroductionView(jmmAppInstallManifest)
      CustomerDivider()
      NewVersionInfoView(jmmAppInstallManifest)
      CustomerDivider()
      OtherInfoView(jmmAppInstallManifest)
      Spacer(modifier = Modifier.height(AppBottomHeight))
    }
  }
}
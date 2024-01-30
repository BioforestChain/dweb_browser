package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.pure.image.compose.CoilAsyncImage

/**
 * 应用介绍的图片展示部分
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaptureListView(
  jmmAppInstallManifest: JmmAppInstallManifest, onSelectPic: (Int, LazyListState) -> Unit
) {
  val lazyListState = rememberLazyListState()
  LazyRow(
    modifier = Modifier.padding(vertical = VerticalPadding),
    state = lazyListState,
    contentPadding = PaddingValues(horizontal = HorizontalPadding)
  ) {
    itemsIndexed(jmmAppInstallManifest.images) { index, item ->
      Card(
        onClick = { onSelectPic(index, lazyListState) },
        modifier = Modifier
          .padding(end = 16.dp)
          .size(ImageWidth, ImageHeight)
      ) {
        CoilAsyncImage(model = item, contentDescription = null)
      }
    }
  }
}
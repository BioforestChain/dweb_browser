package org.dweb_browser.browser.web.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.util.isSystemUrl
import org.dweb_browser.browser.web.data.BrowserContentItem
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.compose.rememberScreenSize

@Composable
fun MultiItemView(
  viewModel: BrowserViewModel,
  browserContentItem: BrowserContentItem,
  onlyOne: Boolean = false,
  index: Int = 0
) {
  val screenSize = rememberScreenSize()
  val scope = rememberCoroutineScope()
  val sizeTriple = if (onlyOne) {
    val with = screenSize.screenWidth.dp - 120.dp
    Triple(with, with * 9 / 6 - 60.dp, with * 9 / 6)
  } else {
    val with = (screenSize.screenWidth.dp - 60.dp) / 2
    Triple(with, with * 9 / 6 - 40.dp, with * 9 / 6)
  }
  Box(modifier = Modifier.size(width = sizeTriple.first, height = sizeTriple.third)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Image(
        painter = browserContentItem.bitmap?.let {
          remember(it) {
            BitmapPainter(it, filterQuality = FilterQuality.Medium)
          }
        } ?: rememberVectorPainter(Icons.Default.BrokenImage),
        contentDescription = null,
        modifier = Modifier
          .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
          .size(width = sizeTriple.first, height = sizeTriple.second)
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .clickable { scope.launch { viewModel.updateMultiViewState(false, index) } }
          .align(Alignment.CenterHorizontally),
        contentScale = ContentScale.FillWidth, //ContentScale.FillBounds,
        alignment = browserContentItem.contentWebItem.value?.let { Alignment.TopStart }
          ?: Alignment.Center
      )
      val homePageTitle = BrowserI18nResource.browser_multi_startup()
      val homePageIcon = BrowserDrawResource.BrowserStar.painter()
      var contentTitle by remember { mutableStateOf(homePageTitle) }
      var contentIcon by remember { mutableStateOf<Painter?>(homePageIcon) }

      LaunchedEffect(browserContentItem) {
        browserContentItem.contentWebItem.value?.viewItem?.let { viewItem ->
          if (!viewItem.webView.getUrl().isSystemUrl()) {
            contentTitle = viewItem.webView.getTitle()
            contentIcon = viewItem.webView.getFavoriteIcon()?.let { BitmapPainter(it) }
          }
        }
      }
      Row(
        modifier = Modifier
          .width(sizeTriple.first)
          .align(Alignment.CenterHorizontally)
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        contentIcon?.let { iconPainter ->
          Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
          )
          Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text = contentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
      }
    }

    if (!onlyOne) {
      Image(
        imageVector = Icons.Default.Close, //ImageVector.vectorResource(R.drawable.ic_circle_close),
        contentDescription = "Close",
        modifier = Modifier
          .clickable { scope.launch { viewModel.closeBrowserContentItem(browserContentItem) } }
          .padding(8.dp)
          .clip(CircleShape)
          .align(Alignment.TopEnd)
          .size(20.dp)
      )
    }
  }
}
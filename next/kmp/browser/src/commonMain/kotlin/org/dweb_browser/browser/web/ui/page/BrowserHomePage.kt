package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.helper.compose.clickableWithNoEffect

@Composable
fun BrowserHomePage.BrowserHomePageRender(modifier: Modifier = Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val scope = rememberCoroutineScope()
  Column(modifier = modifier) {
    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
          painter = BrowserDrawResource.Logo.painter(),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          alignment = Alignment.Center
        )
        Text(
          text = "Dweb Browser",
          fontWeight = FontWeight.Black,
          fontSize = 18.sp
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentAlignment = Alignment.TopCenter
    ) {
      val itemWidth = 80
      val maxRowItemCount = maxWidth.value.toInt().div(itemWidth)
      val gridCells: GridCells
      val gridModifier: Modifier
      if (maxRowItemCount > browserPageList.size) { // 如果大于的话，按照大小显示
        gridModifier = Modifier.width((browserPageList.size * itemWidth).dp)
        gridCells = GridCells.FixedSize(itemWidth.dp)
      } else if (maxRowItemCount == browserPageList.size) { // 如果刚好到话，直接居中显示所有
        gridModifier = Modifier.fillMaxWidth()
        gridCells = GridCells.Fixed(maxRowItemCount)
      } else {
        gridModifier = Modifier.fillMaxWidth()
        gridCells = GridCells.FixedSize(itemWidth.dp)
      }

      LazyVerticalGrid(modifier = gridModifier, columns = gridCells) {
        items(browserPageList) { pageType ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickableWithNoEffect {
              scope.launch { viewModel.tryOpenUrlUI(pageType.url) }
            }
          ) {
            Icon(
              painter = pageType.iconPainter(),
              contentDescription = "download",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .padding(8.dp)
            )
            Text(text = pageType.pageTitle(), maxLines = 1, textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}

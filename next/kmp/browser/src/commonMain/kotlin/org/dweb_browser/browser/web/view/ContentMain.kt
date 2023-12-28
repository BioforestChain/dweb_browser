package org.dweb_browser.browser.web.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.BrowserIconResource
import org.dweb_browser.browser.common.CaptureView
import org.dweb_browser.browser.getIconResource
import org.dweb_browser.browser.web.data.BrowserContentItem
import org.dweb_browser.browser.web.model.BrowserViewModel

@Composable
internal fun BrowserMainView(viewModel: BrowserViewModel, browserContentItem: BrowserContentItem) {
  LaunchedEffect(browserContentItem) {
    delay(200)
    browserContentItem.captureView()
  }

  CaptureView(
    controller = browserContentItem.controller,
    onCaptured = { imageBitmap, _ ->
      imageBitmap?.let { bitmap ->
        browserContentItem.bitmap = bitmap
      }
    }
  ) { HomePage() }
}

@Composable
fun HomePage() {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        getIconResource(BrowserIconResource.BrowserLauncher)!!,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        contentScale = ContentScale.FillWidth
      )
      Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
      Text(text = "Dweb Browser", fontWeight = FontWeight.Black)
    }
  }

  /*Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.align(Alignment.Center)
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      val gradient = listOf(
        Color(0xFF71D78E), Color(0xFF548FE3)
      )
      Text(
        text = "Dweb-Browser",
        style = TextStyle(
          brush = Brush.linearGradient(gradient), fontSize = 36.sp
        ),
        maxLines = 1,
      )
    }
  }*/
}

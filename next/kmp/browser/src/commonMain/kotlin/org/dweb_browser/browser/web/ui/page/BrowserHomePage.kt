package org.dweb_browser.browser.web.ui.page

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserDrawResource


@Composable
fun BrowserHomePageRender(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        BrowserDrawResource.BrowserLauncher.painter(),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        contentScale = ContentScale.FillWidth
      )
      Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
      Text(text = "Dweb Browser", fontWeight = FontWeight.Black)
    }
  }
}

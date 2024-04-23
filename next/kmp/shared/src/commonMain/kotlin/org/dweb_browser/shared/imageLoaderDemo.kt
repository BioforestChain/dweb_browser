package org.dweb_browser.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad

@Composable
fun ImageLoaderDemo() {
  val urls = listOf(
    "https://static.oschina.net/uploads/logo/bun_G8BDG.png",
    "http://know.webhek.com/wp-content/uploads/svg/Ghostscript_Tiger.svg",
    "https://img.alicdn.com/imgextra/i1/O1CN01zDmHYS1e5bjVcOIL7_!!6000000003820-55-tps-16-16.svg",
  );
  Column(
    Modifier.verticalScroll(rememberScrollState())
  ) {
    for (url in urls) {
      Text(url)
      WebCanvasImageLoader(url)
    }
  }
}

@Composable
fun WebCanvasImageLoader(url: String) {
  BoxWithConstraints {
    val imageBitmap = PureImageLoader.SmartLoad(url, maxWidth, maxHeight)
    imageBitmap.with(
      onError = {
        Text(it.stackTraceToString(), color = Color.Red)
      },
      onBusy = {
        Text(it)
      },
    ) {
      Image(it, contentDescription = null)
    }
  }
}

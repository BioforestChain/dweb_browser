package org.dweb_browser.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.helper.platform.offscreenwebcanvas.WebCanvasContextSession.Companion.buildTask
import org.dweb_browser.helper.platform.offscreenwebcanvas.waitReady

@Composable
fun ImageLoaderDemo(webCanvas: OffscreenWebCanvas) {
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
      WebCanvasImageLoader(webCanvas, url)
    }
  }
}

@Composable
fun WebCanvasImageLoader(webCanvas: OffscreenWebCanvas, url: String) {
  BoxWithConstraints {
    val density = LocalDensity.current
    val containerWidth = (maxWidth.value * density.density).toInt()
    val containerHeight = (maxHeight.value * density.density).toInt()
    val imageBitmap by produceState(Result.Loading) {
      value = try {
        webCanvas.waitReady()
        value = Result.Rendering;
        val imageBitmap = webCanvas.buildTask {
          renderImage(url, containerWidth, containerHeight)
          toImageBitmap()
        }
        Result.Success(imageBitmap)
      } catch (e: Throwable) {
        Result.Error(e)
      }
    }
    if (imageBitmap.data != null) {
      Image(imageBitmap.data!!, contentDescription = null)
    } else if (imageBitmap.error != null) {
      Text(imageBitmap.error!!.stackTraceToString(), color = Color.Red)
    } else {
      Text(imageBitmap.busy ?: "...")
    }
  }
}

class Result(
  val data: ImageBitmap? = null, val error: Throwable? = null, val busy: String? = null
) {
  companion object {
    val Loading = Result(busy = "加载中")
    val Rendering = Result(busy = "渲染中")
    fun Success(data: ImageBitmap) = Result(data = data)
    fun Error(error: Throwable?) = Result(error = error)
  }
}
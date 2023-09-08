package org.dweb_browser.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.helper.platform.offscreenwebcanvas.drawImage
import org.dweb_browser.helper.platform.offscreenwebcanvas.toDataURL
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.toBase64ByteArray

@Composable
fun ImageLoaderDemo(webCanvas: OffscreenWebCanvas) {
  val urls = listOf(
    "https://static.oschina.net/uploads/logo/bun_G8BDG.png",
    "http://know.webhek.com/wp-content/uploads/svg/Ghostscript_Tiger.svg",
    "https://img.alicdn.com/imgextra/i1/O1CN01zDmHYS1e5bjVcOIL7_!!6000000003820-55-tps-16-16.svg",
  );
  Column {
    for (url in urls) {
      Text(url)
      WebCanvasImageLoader(webCanvas, url)
    }
  }
}

@Composable
fun WebCanvasImageLoader(webCanvas: OffscreenWebCanvas, url: String) {
  val imageBitmap by produceState(Result.Loading) {
    value = try {
      webCanvas.drawImage(url, dw = 300, dh = 200, resizeCanvas = true)
      val dataUrl = webCanvas.toDataURL()
      val imageBitmap =
        dataUrl.substring("data:image/png;base64,".length).toBase64ByteArray().toImageBitmap()
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
    Text("加载中")
  }
}

class Result(val data: ImageBitmap? = null, val error: Throwable? = null) {
  companion object {
    val Loading = Result()
    fun Success(data: ImageBitmap) = Result(data, null)
    fun Error(error: Throwable?) = Result(null, error)
  }
}
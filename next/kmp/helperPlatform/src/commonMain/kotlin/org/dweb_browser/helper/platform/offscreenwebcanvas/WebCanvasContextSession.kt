package org.dweb_browser.helper.platform.offscreenwebcanvas


import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.platform.OffscreenWebCanvas
import org.dweb_browser.helper.platform.toImageBitmap

suspend fun OffscreenWebCanvas.waitReady() = core.channel.waitReady()
class WebCanvasContextSession private constructor(internal val core: OffscreenWebCanvasCore) {
  companion object {
    suspend fun <T> OffscreenWebCanvas.buildTask(builder: suspend WebCanvasContextSession.() -> T): T {
      return WebCanvasContextSession(core).builder()
    }
  }

  private var jsCode = mutableListOf<String>();
  private fun getExecCode(): String {
    val evalCode = jsCode.joinToString("\n");
    jsCode.clear()
    return evalCode
  }


  fun resize(width: Int, height: Int) {
    jsCode += "canvas.width=$width;canvas.height=$height;\n"
  }

  private fun drawImageWithArgs(
    imageExp: String,
    argsCode: String,
    resizeCanvas: Boolean,
  ) {
    jsCode += listOf(
      "const img=${imageExp};", // 加载图片
      /// 如果配置了resize，那么根据图片大小对画布进行重置
      (if (resizeCanvas) """
            canvas.width=img.width;
            canvas.height=img.height;
            ctx.clearRect(0,0,img.width,img.height);
          """.trimIndent()
      else ""),
      "ctx.drawImage(img,$argsCode);",// 绘制图片到画布中
    )
  }

  fun drawImage(
    imageUri: String, dx: Int = 0, dy: Int = 0, resizeCanvas: Boolean = false
  ) = drawImageWithArgs(imageUriToImageBitmap(imageUri), "$dx,$dy", resizeCanvas)

  fun drawImage(
    imageUri: String, dx: Int = 0, dy: Int = 0, dw: Int, dh: Int, resizeCanvas: Boolean = false
  ) = drawImageWithArgs(
    imageUriToImageBitmap(
      imageUri, ImageBitmapOptions(resizeWidth = dw, resizeHeight = dh)
    ), "$dx,$dy,$dw,$dh", resizeCanvas
  )

  fun drawImage(
    imageUri: String,
    sx: Int,
    sy: Int,
    sw: Int,
    sh: Int,
    dx: Int,
    dy: Int,
    dw: Int,
    dh: Int,
    resizeCanvas: Boolean = false
  ) = drawImageWithArgs(
    imageUriToImageBitmap(imageUri), "$sx,$sy,$sw,$sh,$dx,$dy,$dw,$dh", resizeCanvas
  )

  private fun imageUriToImageBitmap(imageUri: String, options: ImageBitmapOptions? = null) =
    /// fetch 如果使用 mode:'no-cors'，那么blob始终为空，所以匿名模式没有意义
    "(await fetchImageBitmap(wrapUrlByProxy(`$imageUri`)${
      if (options == null) "" else ",${
        Json.encodeToString(
          options
        )
      }"
    }))"

  suspend fun toDataURL(type: String = "image/png", quality: Float = 1.0f): String {
    jsCode += "return canvasToDataURL(canvas,{type:`$type`,quality:$quality});"
    return core.evalJavaScriptReturnString(getExecCode())
  }

  suspend fun toImageBitmap(type: String = "image/png", quality: Float = 1.0f): ImageBitmap {
    jsCode += "return canvasToBlob(canvas,{type:`$type`,quality:$quality});"
    return core.evalJavaScriptReturnByteArray(getExecCode()).toImageBitmap()
  }

  fun clearRect(x: Int, y: Int, w: Int, h: Int) {
    jsCode += "ctx.clearRect($x,$y,$w,$h);"
  }

}

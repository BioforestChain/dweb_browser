package org.dweb_browser.helper.platform.offscreenwebcanvas


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.platform.OffscreenWebCanvas

suspend fun OffscreenWebCanvas.resize(width: Int, height: Int) =
  evalJavaScriptWithVoid("canvas.width=$width;canvas.height=$height;").getOrThrow()

private suspend fun OffscreenWebCanvas.drawImageWithArgs(
  imageUri: String,
  argsCode: String,
  resizeCanvas: Boolean,
) {
  evalJavaScriptWithVoid(
    /// 加载图片
    "const img=${imageUriToImageBitmap(imageUri)};" +
        /// 如果配置了resize，那么根据图片大小对画布进行重置
        (if (resizeCanvas) """
            canvas.width=img.width;
            canvas.height=img.height;
            ctx.clearRect(0,0,img.width,img.height);
          """.trimIndent()
        else "") +
        /// 绘制图片到画布中
        "ctx.drawImage(img,$argsCode);"
  ).getOrThrow()
}

suspend fun OffscreenWebCanvas.drawImage(
  imageUri: String, dx: Int = 0, dy: Int = 0, resizeCanvas: Boolean = false
) = drawImageWithArgs(imageUri, "$dx,$dy", resizeCanvas)

suspend fun OffscreenWebCanvas.drawImage(
  imageUri: String, dx: Int = 0, dy: Int = 0, dw: Int, dh: Int, resizeCanvas: Boolean = false
) = drawImageWithArgs(imageUri, "$dx,$dy,$dw,$dh", resizeCanvas)

suspend fun OffscreenWebCanvas.drawImage(
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
) = drawImageWithArgs(imageUri, "$sx,$sy,$sw,$sh,$dx,$dy,$dw,$dh", resizeCanvas)

private fun imageUriToImageBitmap(imageUri: String) =
  /// fetch 如果使用 mode:'no-cors'，那么blob始终为空，所以匿名模式没有意义
  "(await fetchImageBitmap(wrapUrlByProxy(`$imageUri`)))"

@Serializable
data class ImageMetadata(val width: Int, val height: Int)

suspend fun OffscreenWebCanvas.loadImage(key: String, imageUri: String) = evalJavaScriptWithResult(
  """
  const img=${imageUriToImageBitmap(imageUri)}
  return {width:img.width,height:img.height}
""".trimIndent()
).getOrThrow().let { Json.decodeFromString<ImageMetadata>(it) }

suspend fun OffscreenWebCanvas.toDataURL(type: String = "image/png", quality: Float = 1.0f) =
  evalJavaScriptWithResult("return canvasToDataURL(canvas,{type:`$type`,quality:$quality})").getOrThrow()

suspend fun OffscreenWebCanvas.clearRect(x: Int, y: Int, w: Int, h: Int) =
  evalJavaScriptWithVoid("ctx.clearRect($x,$y,$w,$h)").getOrThrow()
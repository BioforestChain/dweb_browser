package org.dweb_browser.helper.platform.offscreenwebcanvas


import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.StringEnumSerializer
import org.dweb_browser.helper.platform.OffscreenWebCanvas

suspend fun OffscreenWebCanvas.resize(width: Int, height: Int) =
  evalJavaScriptWithVoid("canvas.width=$width;canvas.height=$height;").getOrThrow()

private suspend fun OffscreenWebCanvas.drawImageWithArgs(
  imageExp: String,
  argsCode: String,
  resizeCanvas: Boolean,
) {
  evalJavaScriptWithVoid(
    /// 加载图片
    "const img=${imageExp};" +
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
) = drawImageWithArgs(imageUriToImageBitmap(imageUri), "$dx,$dy", resizeCanvas)

suspend fun OffscreenWebCanvas.drawImage(
  imageUri: String, dx: Int = 0, dy: Int = 0, dw: Int, dh: Int, resizeCanvas: Boolean = false
) = drawImageWithArgs(
  imageUriToImageBitmap(
    imageUri,
    ImageBitmapOptions(resizeWidth = dw, resizeHeight = dh)
  ), "$dx,$dy,$dw,$dh", resizeCanvas
)

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
) = drawImageWithArgs(
  imageUriToImageBitmap(imageUri),
  "$sx,$sy,$sw,$sh,$dx,$dy,$dw,$dh",
  resizeCanvas
)

@Serializable
public data class ImageBitmapOptions(
  val colorSpaceConversion: ColorSpaceConversion? = null,
  val imageOrientation: ImageOrientation? = null,
  val premultiplyAlpha: PremultiplyAlpha? = null,
  val resizeHeight: Int? = null,
  val resizeQuality: Float? = null,
  val resizeWidth: Int? = null,
) {
  @Serializable
  enum class ColorSpaceConversion {
    default, none
  }

  object ImageOrientationSerializer : StringEnumSerializer<ImageOrientation>(
    "ImageOrientation",
    ImageOrientation.entries.associateBy { it.value },
    { value })

  @Serializable(ImageOrientationSerializer::class)
  enum class ImageOrientation(val value: String) {
    none("none"), flipY("flipY"), fromImage("from-image")
  }

  @Serializable
  enum class PremultiplyAlpha {
    default, none, premultiply
  }
}

private fun imageUriToImageBitmap(imageUri: String, options: ImageBitmapOptions? = null) =
  /// fetch 如果使用 mode:'no-cors'，那么blob始终为空，所以匿名模式没有意义
  "(await fetchImageBitmap(wrapUrlByProxy(`$imageUri`)${
    if (options == null) "" else ",${
      Json.encodeToString(
        options
      )
    }"
  }))"

@Serializable
data class ImageMetadata(val width: Int, val height: Int)

//suspend fun OffscreenWebCanvas.loadImage(key: String, imageUri: String) = evalJavaScriptWithResult(
//  """
//  const img=${imageUriToImageBitmap(imageUri)}
//  return {width:img.width,height:img.height}
//""".trimIndent()
//).getOrThrow().let { Json.decodeFromString<ImageMetadata>(it) }

suspend fun OffscreenWebCanvas.toDataURL(type: String = "image/png", quality: Float = 1.0f) =
  evalJavaScriptWithResult("return canvasToDataURL(canvas,{type:`$type`,quality:$quality})").getOrThrow()

suspend fun OffscreenWebCanvas.clearRect(x: Int, y: Int, w: Int, h: Int) =
  evalJavaScriptWithVoid("ctx.clearRect($x,$y,$w,$h)").getOrThrow()
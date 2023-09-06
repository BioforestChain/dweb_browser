package org.dweb_browser.helper.platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

expect class OffscreenWebCanvas(width: Int, height: Int) {
  companion object {
    val Default: OffscreenWebCanvas
  }

  suspend fun evalJavaScriptWithResult(jsCode: String): Result<String>
  suspend fun evalJavaScriptWithVoid(jsCode: String): Result<Unit>
  val width: Int
  val height: Int

}

suspend fun OffscreenWebCanvas.clearRect(x: Int, y: Int, w: Int, h: Int) =
  evalJavaScriptWithVoid("ctx.clearRect($x,$y,$w,$h)").getOrThrow()

suspend fun OffscreenWebCanvas.resize(width: Int, height: Int) =
  evalJavaScriptWithVoid("canvas.width=$width;canvas.height=height;").getOrThrow()

private suspend fun OffscreenWebCanvas.drawImageWithArgs(imageUri: String, argsCode: String) {
    evalJavaScriptWithVoid("""
      const img=imageUriToHTMLImageElementCode(imageUri)
      ctx.drawImage(img,$argsCode)
    """.trimIndent()).getOrThrow()
}
suspend fun OffscreenWebCanvas.drawImage(imageUri: String, dx: Int = 0, dy: Int = 0) =

  evalJavaScriptWithVoid("ctx.drawImage(${imageUriToHTMLImageElementCode(imageUri)},$dx,$dy)").getOrThrow()

suspend fun OffscreenWebCanvas.drawImage(
  imageUri: String, dx: Int = 0, dy: Int = 0, dw: Int, dh: Int
) =

  evalJavaScriptWithVoid("ctx.drawImage(${imageUriToHTMLImageElementCode(imageUri)},$dx,$dy,$dw,$dh)").getOrThrow()

suspend fun OffscreenWebCanvas.drawImage(
  imageUri: String, sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, dw: Int, dh: Int
) =
  evalJavaScriptWithVoid("ctx.drawImage(`$imageUri`,$sx,$sy,$sw,$sh,$dx,$dy,$dw,$dh)").getOrThrow()

private fun imageUriToHTMLImageElementCode(imageUri: String) = """
  await new Promise((resolve,reject)=>{
    const img=new Image();
    img.src=`$imageUri`;
    img.onload=()=>resolve(img);
    img.onerror=reject;
  })
""".trimIndent()

@Serializable
data class ImageMetadata(val width: Int, val height: Int)

suspend fun OffscreenWebCanvas.loadImage(key: String, imageUri: String) = evalJavaScriptWithResult(
  """
  const img=${imageUriToHTMLImageElementCode(imageUri)}
  return {width:img.width,height:img.height}
""".trimIndent()
).getOrThrow().let { Json.decodeFromString<ImageMetadata>(it) }

suspend fun OffscreenWebCanvas.toDataURL(type: String = "image/png", quality: Float = 1.0f) =
  evalJavaScriptWithResult("return canvas.toDataURL(`$type`,$quality)").getOrThrow()

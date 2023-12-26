package org.dweb_browser.pure.image.offscreenwebcanvas


import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.pure.image.OffscreenWebCanvas

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


  fun renderImage(imageUri: String, containerWidth: Int, containerHeight: Int) {
    jsCode += "const img=${fetchImageBitmap(imageUri, containerWidth, containerHeight)};"; // 加载图片
    /// 根据图片大小对画布进行重置
    jsCode += """
    canvas.width=img.width;
    canvas.height=img.height;
    ctx.clearRect(0,0,img.width,img.height);
    """.trimIndent();
    jsCode += "ctx.drawImage(img,0,0,img.width,img.height);" // 绘制图片到画布中
  }

  private fun fetchImageBitmap(imageUri: String, containerWidth: Int, containerHeight: Int) =
    /// fetch 如果使用 mode:'no-cors'，那么blob始终为空，所以匿名模式没有意义
    "(await fetchImageBitmap(wrapUrlByProxy(`$imageUri`),$containerWidth,$containerHeight))"

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

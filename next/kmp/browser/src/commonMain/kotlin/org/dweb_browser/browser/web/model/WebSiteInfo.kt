package org.dweb_browser.browser.web.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.web.ui.capture.CaptureController
import org.dweb_browser.dwebview.base.ViewItem

interface BrowserBaseView {
  val show: MutableState<Boolean> // 用于首页是否显示遮罩
  val focus: MutableState<Boolean> // 用于搜索框显示的内容，根据是否聚焦来判断
  val controller: CaptureController
  var bitmap: ImageBitmap?
}

data class BrowserMainView(
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  override val controller: CaptureController = CaptureController(),
  override var bitmap: ImageBitmap? = null,
) : BrowserBaseView

data class BrowserWebView(
  val viewItem: ViewItem,
  override val show: MutableState<Boolean> = mutableStateOf(true),
  override val focus: MutableState<Boolean> = mutableStateOf(false),
  override val controller: CaptureController = CaptureController(),
  override var bitmap: ImageBitmap? = null,
  val loadState: MutableState<Boolean> = mutableStateOf(false),
) : BrowserBaseView {
  val closeWatcher get() = viewItem.webView.closeWatcher
}

data class HotspotInfo(
  val id: Int = 0,
  val name: String,
  val webUrl: String,
  val iconUrl: String = "",
) {
  fun showHotText(): AnnotatedString {
    val color = when (id) {
      1 -> Color.Red
      2 -> Color(0xFFFF6C2D)
      3 -> Color(0xFFFF6C2D)
      else -> Color.LightGray
    }
    return buildAnnotatedString {
      withStyle(
        style = SpanStyle(
          color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
      ) {
        append("$id".padEnd(5, ' '))
      }
      withStyle(
        style = SpanStyle(
          color = Color.Black,
          fontSize = 16.sp
        )
      ) {
        append(name)
      }
    }
  }
}
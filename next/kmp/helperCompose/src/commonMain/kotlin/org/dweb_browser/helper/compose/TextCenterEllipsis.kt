package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

/**
 * 文字太长的时候，中间显示三个缩略号
 */
@Composable
fun TextCenterEllipsis(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontStyle: FontStyle? = null,
  fontWeight: FontWeight? = null,
  fontFamily: FontFamily? = null,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign = TextAlign.Unspecified,
  lineHeight: TextUnit = TextUnit.Unspecified,
  style: TextStyle = LocalTextStyle.current
) {
  val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

  val textStyle = style.merge(
    color = textColor,
    fontSize = fontSize,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    lineHeight = lineHeight
  )
  BoxWithConstraints(modifier = modifier) {
    var showText = text
    val maxWidthPX = maxWidth.value * LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()
    val layoutResult = textMeasurer.measure(text, style = textStyle)
    if (layoutResult.size.width >= maxWidthPX.roundToInt()) {
      for (length in text.length - 3 downTo 1) { // 由于中间会多"..."，所以长度直接从length-3开始
        // 根据奇偶数进行左右边的截取计算
        val (left, right) = if (length.mod(2) == 0) {
          Pair(length / 2, text.length - length / 2)
        } else Pair((length + 1) / 2, text.length - (length - 1) / 2)
        // 拼凑出子字符串
        val substring = text.substring(0, left) + "..." + text.substring(right)
        val substringLayoutResult = textMeasurer.measure(text = substring, style = textStyle)
        if (substringLayoutResult.size.width < maxWidthPX.roundToInt()) {
          showText = substring
          break
        }
      }
    }
    Text(
      modifier = Modifier.fillMaxWidth(),
      text = showText,
      overflow = TextOverflow.Clip,
      maxLines = 1,
      style = textStyle
    )
  }
}
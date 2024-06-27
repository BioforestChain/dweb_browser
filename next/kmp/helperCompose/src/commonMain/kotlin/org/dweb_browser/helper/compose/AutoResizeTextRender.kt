package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * 自适应伸缩的文本容器
 *
 * 这里提供一个快速的计算，如果你真正避免文字溢出的问题，请配合 AutoResizeText 进行使用
 */
@Composable
fun AutoResizeTextContainer(
  modifier: Modifier = Modifier,
  content: @Composable @UiComposable (AutoResizeTextContainerScope.() -> Unit),
) {
  BoxWithConstraints(modifier) {
    val scope = remember(maxWidth, maxHeight) {
      AutoResizeTextContainerScope(
        maxWidth.value, maxHeight.value, this
      )
    }
    scope.content()
  }
}

@Composable
fun AutoResizeTextContainer(
  modifier: Modifier = Modifier,
  contentAlignment: Alignment = Alignment.TopStart,
  content: @Composable @UiComposable (AutoResizeTextContainerScope.() -> Unit),
) {
  BoxWithConstraints(modifier, contentAlignment) {
    val scope = remember(maxWidth, maxHeight) {
      AutoResizeTextContainerScope(
        maxWidth.value, maxHeight.value, this
      )
    }
    scope.content()
  }
}

/**
 * 上下文
 */
class AutoResizeTextContainerScope(
  var maxWidth: Float,
  var maxHeight: Float,
  private val boxScope: BoxWithConstraintsScope,
) : BoxScope {
  fun calc(
    text: String,
    /**
     * 计算所用的文字宽高比
     * 一般来说 英文文字 宽高比不会超过 0.6，如果文字中包含中文，请使用 0.9/1 的比例
     */
    fontRatio: Float = 0.6f,
    /**
     * 行高比例
     */
    lineHeightRatio: Float = 1.5f,
  ): Float {
    val textCount = max(text.length, 0)
    fun calcTextSizeByLines(lines: Int): Float {
      val lineSize = maxHeight / (lines * lineHeightRatio - (lineHeightRatio - 1))
      return lineSize * fontRatio
    }

    /// 根据行数计算字体大小
    var lines = 1
    while (true) {
      // 首先计算出这个行数情况下能否满足填充需求
      val minTextSize = calcTextSizeByLines(lines + 1)
      val maxTextCount = (maxWidth / minTextSize) * lines
      if (maxTextCount < textCount && lines <= 3) { // 超过三行跳出循环，否则会引起死循环
        // 这个行数下，还是放不下那么多文字，那么就增加行数，继续循环
        lines += 1
        continue
      }

      // 在这个行数下，那么尽可能地放大字体
      val maxTextSize = calcTextSizeByLines(lines)
      val tryUnit = max(0.1f, (maxTextSize - minTextSize) / 5)// 5个档位
      var bestTextSize = minTextSize
      while (bestTextSize < maxTextSize) {
        val tryTextSize = bestTextSize + tryUnit
        val tryTextCount = (maxWidth / tryTextSize) * lines
        if (tryTextCount >= textCount) {
          bestTextSize = tryTextSize
          continue
        }
        break
      }
      return bestTextSize
    }
  }

  override fun Modifier.align(alignment: Alignment): Modifier {
    return with(boxScope) {
      this@align.align(alignment)
    }
  }

  override fun Modifier.matchParentSize(): Modifier {
    return with(boxScope) {
      this@matchParentSize.matchParentSize()
    }
  }

}

class AutoResizeContext(var fontSize: TextUnit, var lightHeight: TextUnit)

@Composable
fun AutoResizeTextContainerScope.AutoSizeText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontStyle: FontStyle? = null,
  fontWeight: FontWeight? = null,
  fontFamily: FontFamily? = null,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  lineHeight: TextUnit = TextUnit.Unspecified,
  overflow: TextOverflow = TextOverflow.Clip,
  softWrap: Boolean = true,
  maxLines: Int = Int.MAX_VALUE,
  style: TextStyle = LocalTextStyle.current,
  autoResizeEnabled: Boolean = true,
  onResize: (AutoResizeContext.() -> Unit)? = null,
) {
  var readyToDraw by remember(
    maxWidth, maxHeight, autoResizeEnabled
  ) { mutableStateOf(!autoResizeEnabled) }
  var fontSizeValue by remember(text, maxWidth, maxHeight, autoResizeEnabled) {
    val maxFontSizeValue = calc(text.text)
    val customFontSizeValue = when (fontSize.value) {
      Float.NaN -> style.fontSize.value
      else -> fontSize.value
    }
    if (customFontSizeValue.isNaN()) {
      mutableFloatStateOf(maxFontSizeValue)
    } else {
      mutableFloatStateOf(min(maxFontSizeValue, customFontSizeValue))
    }
  }
  val context = remember {
    AutoResizeContext(TextUnit.Unspecified, lineHeight)
  }
  context.fontSize = fontSizeValue.sp
  context.lightHeight = lineHeight
  onResize?.invoke(context)

  Text(modifier = modifier.drawWithContent {
    if (readyToDraw) drawContent()
  },
    text = text,
    color = color,
    fontSize = context.fontSize,
    lineHeight = context.lightHeight,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    style = style,
    onTextLayout = { textLayoutResult ->
      if (autoResizeEnabled && (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight)) {
        fontSizeValue *= 0.9f
      } else {
        readyToDraw = true
      }
    })
}


@Composable
fun AutoResizeTextContainerScope.AutoSizeText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontStyle: FontStyle? = null,
  fontWeight: FontWeight? = null,
  fontFamily: FontFamily? = null,
  letterSpacing: TextUnit = TextUnit.Unspecified,
  textDecoration: TextDecoration? = null,
  textAlign: TextAlign? = null,
  lineHeight: TextUnit = TextUnit.Unspecified,
  overflow: TextOverflow = TextOverflow.Clip,
  softWrap: Boolean = true,
  maxLines: Int = Int.MAX_VALUE,
  style: TextStyle = LocalTextStyle.current,
  autoResizeEnabled: Boolean = true,
  onResize: (AutoResizeContext.() -> Unit)? = null,
) {
  AutoSizeText(
    modifier = modifier,
    text = AnnotatedString(text),
    color = color,
    fontSize = fontSize,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    lineHeight = lineHeight,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    style = style,
    autoResizeEnabled = autoResizeEnabled,
    onResize = onResize,
  )
}
package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun SimpleBox() {
  Box(modifier = Modifier.background(Color.Red).size(30.dp).clip(squircleshape.SquircleShape())) { }
}

/**
 * 自适应伸缩的文本容器
 *
 * 这里提供一个快速的计算，如果你真正避免文字溢出的问题，请配合 AutoResizeText 进行使用
 */
@Composable
fun AutoResizeTextContainer(
  modifier: Modifier = Modifier,
  content: @Composable @UiComposable() (AutoResizeTextContainerScope.() -> Unit)
) {
  BoxWithConstraints(modifier) {
    val scope = remember {
      AutoResizeTextContainerScope(
        maxWidth.value, maxHeight.value, this
      )
    }
    scope.maxWidth = maxWidth.value
    scope.maxHeight = maxHeight.value
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
    fontRatio: Float = 0.6f
  ): Float {
    /// 根据面积计算字体大小
    val maxArea = maxWidth * maxHeight
    val textMaxUnit = maxArea / text.length //  min(textWidthUnit, textHeightUnit)
    /// 计算出最大的文字宽高
    return min(sqrt(textMaxUnit * fontRatio), min(maxWidth * 0.9f, maxHeight * 0.8f))
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
  autoLineHeight: ((fontSize: TextUnit) -> TextUnit)? = null
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
  val resizedFontSize = remember(fontSizeValue) {
    fontSizeValue.sp
  }
  val resizedLineHeight = remember(fontSizeValue) {
    when (autoLineHeight) {
      null -> lineHeight
      else -> autoLineHeight(resizedFontSize)
    }
  }
//  Text(modifier = Modifier
//    .align(Alignment.Center)
//    .padding(2.dp)
//    .drawWithContent {
//      if (readyToDraw) drawContent()
//    },
//    text = titleText,
//    textAlign = TextAlign.Center,
//    style = fontStyle,
//    onTextLayout = { textLayoutResult ->
//      if (!inResize && (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight)) {
//        fontSize *= 0.9f
//      } else {
//        readyToDraw = true
//      }
//    })

  Text(modifier = modifier.drawWithContent {
    if (readyToDraw) drawContent()
  },
    text = text,
    color = color,
    fontSize = resizedFontSize,
    lineHeight = resizedLineHeight,
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
  autoLineHeight: ((fontSize: TextUnit) -> TextUnit)? = null
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
    autoLineHeight = autoLineHeight,
  )
}
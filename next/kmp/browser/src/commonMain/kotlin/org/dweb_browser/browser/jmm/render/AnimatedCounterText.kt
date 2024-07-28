package org.dweb_browser.browser.jmm.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import org.dweb_browser.helper.compose.iosTween

@Composable
fun AnimatedCounterText(
  text: String, modifier: Modifier = Modifier, textStyle: TextStyle? = null,
) {
  Row(modifier) {
    val style = textStyle ?: LocalTextStyle.current
    var preTextEndIndex = 0;
    for (matchResult in Regex("[\\d.]+").findAll(text)) {
      if (preTextEndIndex < matchResult.range.first) {
        Text(text.substring(preTextEndIndex, matchResult.range.first), style = style)
      }
      AnimatedCounter(count = matchResult.value, textStyle = style)
      preTextEndIndex = matchResult.range.last + 1
    }
    if (preTextEndIndex < text.length - 1) {
      Text(text.substring(preTextEndIndex), style = style)
    }
  }
}

@Composable
private fun AnimatedCounter(count: String, textStyle: TextStyle? = null) {
  Row(
    modifier = Modifier.animateContentSize(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val style = textStyle ?: LocalTextStyle.current
    val countNum = count.toFloat()
    count.mapIndexed { index, c -> Digit(c, countNum, index) }.forEach { digit ->
      val animationSpec: FiniteAnimationSpec<IntOffset> =
        iosTween(durationMillis = 100 + digit.place * 50)
      key(count.length - digit.place) {
        AnimatedContent(targetState = digit, transitionSpec = {
          if (targetState > initialState) {
            slideInVertically(animationSpec) { -it } togetherWith slideOutVertically(animationSpec) { it }
          } else {
            slideInVertically(animationSpec) { it } togetherWith slideOutVertically(animationSpec) { -it }
          }
        }) { digit ->
          Text("${digit.digitChar}", style = style)
        }
      }
    }
  }
}

private data class Digit(val digitChar: Char, val fullNumber: Float, val place: Int)

private operator fun Digit.compareTo(other: Digit): Int {
  return fullNumber.compareTo(other.fullNumber)
}
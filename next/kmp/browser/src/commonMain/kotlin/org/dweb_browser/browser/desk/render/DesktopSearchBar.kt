package org.dweb_browser.browser.desk.render

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.helper.compose.NativeBackHandler

private fun <T> aniSpec() = spring<T>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)

private val outlineShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopSearchBar(
  value: String,
  onSearch: (String) -> Unit,
//  onKeyboardDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  var isFocused by remember { mutableStateOf(false) }
  var searchText by remember(value) {
    mutableStateOf(TextFieldValue(value))
  }
  val bgColorAni = remember { Animatable(Color.White.copy(alpha = 0.2f)) }
  LaunchedEffect(isFocused) {
    if (isFocused) {
      searchText = TextFieldValue(searchText.text, TextRange(0, searchText.text.length))
    }
    bgColorAni.animateTo(
      when {
        isFocused -> Color.Black.copy(alpha = 0.2f)
        else -> Color.White.copy(alpha = 0.2f)
      }, aniSpec()
    )
  }

  Box(modifier, contentAlignment = Alignment.Center) {
    BasicTextField(
      searchText,
      onValueChange = { searchText = it },
      modifier = Modifier
        // 布局与大小
        .padding(horizontal = 8.dp).height(48.dp)
        // 样式
        .background(color = bgColorAni.value, shape = outlineShape)
        .border(1.dp, color = Color.White, shape = outlineShape).animateContentSize(aniSpec())
        // 行为
        .onFocusChanged {
          isFocused = it.isFocused

        },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = {
        onSearch(searchText.text)
//      onKeyboardDismiss()
        focusManager.clearFocus()
        searchText = TextFieldValue("")
      }),
      decorationBox = { innerTextField ->
        NativeBackHandler(isFocused) {
          focusManager.clearFocus()
        }
        LazyRow(
          Modifier.padding(start = 12.dp, end = 16.dp).sizeIn(minWidth = 120.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          item {
            val iconScaleAni = remember { Animatable(0f) }
            LaunchedEffect(isFocused) {
              iconScaleAni.animateTo(if (isFocused) 24f / 32f else 1f, aniSpec())
            }
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.animateItem(placementSpec = aniSpec())
                /// 这里不直接操作size，避免布局计算异常导致崩溃
                .size(32.dp).scale(iconScaleAni.value),
            )
          }
          item {
            Box(
              contentAlignment = Alignment.CenterStart,
              modifier = Modifier.padding(start = 4.dp).animateItem(placementSpec = aniSpec())
                .composed { if (isFocused) fillMaxWidth() else width(0.dp) }.fillMaxHeight()
            ) {
              innerTextField()
            }
          }
        }
      },
      textStyle = TextStyle(Color.White, fontSize = 18.sp),
      cursorBrush = SolidColor(Color.White),
    )
  }

}
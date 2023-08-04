package info.bagen.dwebbrowser.microService.browser.desk

import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.ui.view.findActivity
import org.dweb_browser.dwebview.DWebView
import kotlin.math.roundToInt

val LocalInstallList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<DeskAppMetaData>> {
  noLocalProvidedFor("LocalOpenList")
}

val LocalDesktopView = compositionLocalOf<DWebView> {
  noLocalProvidedFor("DesktopView")
}

private fun noLocalProvidedFor(name: String): Nothing {
  error("CompositionLocal $name not present")
}

@Composable
fun FloatTaskbarView(
  url: String,
  state: MutableState<Boolean> = mutableStateOf(true),
  isFloatWindow: Boolean = true,
  width: Dp = 72.dp,
  height: Dp = 72.dp,
  clipSize: Dp = 16.dp
) {
  if (state.value) {
    val localTaskbarModel = LocalContext.current.findActivity().let {
      val model by it.taskAppViewModels<TaskbarViewModel>()
      model
    }
    FloatBox(isFloatWindow, width, height, clipSize, onClick = {
      // TODO 打开task app，并且将自己状态置为 false
      localTaskbarModel.floatViewState.value = false
      localTaskbarModel.openTaskActivity()
    }) {
      WebView(
        state = rememberWebViewState(url = url)
      ) {
        localTaskbarModel.taskbarDWebView.parent?.let {
          (it as ViewGroup).removeView(localTaskbarModel.taskbarDWebView)
        }
        localTaskbarModel.taskbarDWebView
      }
    }
  }
}

@Composable
private fun FloatBox(
  isFloatWindow: Boolean,
  width: Dp,
  height: Dp,
  clipSize: Dp,
  onClick: () -> Unit,
  content: @Composable () -> Unit
) {
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val density = LocalDensity.current.density
  val rememberOffset =
    remember {
      val offset = Offset(
        if (isFloatWindow) (screenWidth - width).value * density else 0f,
        if (isFloatWindow) 200.dp.value * density else 0f
      )
      mutableStateOf(offset)
    }
  if (isFloatWindow) {
    Box(modifier = Modifier
      .offset {
        IntOffset(rememberOffset.value.x.roundToInt(), rememberOffset.value.y.roundToInt())
      }
      .pointerInput(rememberOffset) {
        detectDragGestures { _, dragAmount ->
          rememberOffset.value = Offset(
            x = rememberOffset.value.x + dragAmount.x, y = rememberOffset.value.y + dragAmount.y
          )
        }
      }
      .size(width, height)
      .clip(RoundedCornerShape(clipSize))
    ) {
      content()
      Box(modifier = Modifier
        .fillMaxSize()
        .clickableWithNoEffect { onClick() })
    }
  } else {
    Box(
      modifier = Modifier
        .size(width, height)
        .clip(RoundedCornerShape(clipSize))
    ) {
      content()
    }
  }
}
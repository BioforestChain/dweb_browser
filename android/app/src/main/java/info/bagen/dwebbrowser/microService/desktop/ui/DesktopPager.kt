package info.bagen.dwebbrowser.microService.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.core.WindowAppInfo
import info.bagen.dwebbrowser.microService.desktop.model.LocalDrawerManager
import info.bagen.dwebbrowser.microService.desktop.model.LocalOpenList
import info.bagen.dwebbrowser.microService.mwebview.MultiWebView
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect

@Composable
internal fun DesktopPager() {
  val localOpenList = LocalOpenList.current
  Box(modifier = Modifier.fillMaxSize()) {
    localOpenList.forEachIndexed { _, appInfo ->
      ToolbarAndWebView(appInfo)
    }
  }
}

@Composable
internal fun ToolbarAndWebView(windowAppInfo: WindowAppInfo) {
  val localOpenList = LocalOpenList.current
  val localDrawerManager = LocalDrawerManager.current
  val triple = when (windowAppInfo.screenType.value) {
    WindowAppInfo.ScreenType.Hide -> {
      Triple(0f, 0f, 0f)
    }

    WindowAppInfo.ScreenType.Half -> {
      Triple(windowAppInfo.zoom.value, windowAppInfo.offsetX.value, windowAppInfo.offsetY.value)
    }

    WindowAppInfo.ScreenType.Full -> {
      Triple(1f, 0f, 0f)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer(
        scaleX = triple.first,
        scaleY = triple.first,
        translationX = triple.second,
        translationY = triple.third
      )
      .pointerInput(windowAppInfo) {
        detectTransformGestures { centroid, pan, zoom, rotation ->
          if (windowAppInfo.screenType.value == WindowAppInfo.ScreenType.Half) windowAppInfo.zoom.value *= zoom
        }
      }
      .clickableWithNoEffect {
        localOpenList.remove(windowAppInfo)
        localOpenList.add(windowAppInfo)
      }
  ) {


    Box(
      modifier = Modifier
        .fillMaxSize()
        .align(Alignment.TopStart)
        .background(Color.Black)
        .padding(1.dp)
        .background(MaterialTheme.colorScheme.background)
    ) {
      windowAppInfo.viewItem?.let { multiViewItem ->
        MultiWebView(mmid = windowAppInfo.jsMicroModule.mmid, viewItem = multiViewItem)
      }
    }
    Box(modifier = Modifier
      .pointerInput(windowAppInfo) {
        detectTransformGestures { centroid, pan, zoom, rotation ->
          if (windowAppInfo.screenType.value == WindowAppInfo.ScreenType.Half) {
            windowAppInfo.offsetX.value += pan.x * windowAppInfo.zoom.value
            windowAppInfo.offsetY.value += pan.y * windowAppInfo.zoom.value
          }
        }
      }
      .clickableWithNoEffect {
        if (localDrawerManager.visibleState.targetState) localDrawerManager.hide() else localDrawerManager.show()
      }) {
      Toolbar(
        windowAppInfo = windowAppInfo,
        onClose = { localOpenList.remove(windowAppInfo) },
        onExpand = {
          if (windowAppInfo.screenType.value == WindowAppInfo.ScreenType.Full) {
            windowAppInfo.screenType.value = WindowAppInfo.ScreenType.Half
            windowAppInfo.expand = false
          } else {
            windowAppInfo.screenType.value = WindowAppInfo.ScreenType.Full
            windowAppInfo.expand = true
          }
        }
      )
    }
  }
}

@Composable
internal fun Toolbar(
  windowAppInfo: WindowAppInfo,
  modifier: Modifier = Modifier,
  onClose: () -> Unit,
  onExpand: () -> Unit
) {
  TopAppBar(modifier = modifier) {
    Icon(
      imageVector = ImageVector.vectorResource(id = R.drawable.ic_main_close),
      contentDescription = "close",
      modifier = Modifier
        .size(32.dp)
        .clickableWithNoEffect { onClose() }
    )
    Text(
      text = windowAppInfo.jsMicroModule.metadata.name,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.Center,
      maxLines = 1
    )
    Icon(
      imageVector = ImageVector.vectorResource(
        if (windowAppInfo.screenType.value == WindowAppInfo.ScreenType.Full) R.drawable.ic_shrink else R.drawable.ic_expand
      ),
      contentDescription = "FullScreen",
      modifier = Modifier
        .size(32.dp)
        .clickableWithNoEffect { onExpand() }
    )
  }
}

@Composable
internal fun SmallPillView(name: String) { // 小药丸视图
  Row(
    modifier = Modifier
      .height(48.dp)
      .widthIn(min = 50.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.background),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {

    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.ic_main_close),
      contentDescription = "Close",
      modifier = Modifier.weight(1f)
    )

    Spacer(
      modifier = Modifier
        .width(1.dp)
        .height(36.dp)
        .background(MaterialTheme.colorScheme.onBackground)
    )

    Text(
      text = name,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.Center
    )
  }
}

@Preview
@Composable
internal fun SmallPillPreview() {
  SmallPillView(name = "我的应用")
}



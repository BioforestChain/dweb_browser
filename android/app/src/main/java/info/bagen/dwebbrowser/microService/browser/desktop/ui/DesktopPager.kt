package info.bagen.dwebbrowser.microService.browser.desktop.ui

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
import info.bagen.dwebbrowser.microService.browser.desktop.DeskAppMetaData
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalDrawerManager
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalOpenList
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebView
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
internal fun ToolbarAndWebView(deskAppMetaData: DeskAppMetaData) {
  val localOpenList = LocalOpenList.current
  val localDrawerManager = LocalDrawerManager.current
//  val triple = when (deskAppMetaData.screenType.value) {
//    DeskAppMetaData.ScreenType.Hide -> {
//      Triple(0f, 0f, 0f)
//    }
//
//    DeskAppMetaData.ScreenType.Half -> {
//      Triple(deskAppMetaData.zoom.value, deskAppMetaData.offsetX.value, deskAppMetaData.offsetY.value)
//    }
//
//    DeskAppMetaData.ScreenType.Full -> {
//      Triple(1f, 0f, 0f)
//    }
//  }

  Box(
    modifier = Modifier
      .fillMaxSize()
//      .graphicsLayer(
//        scaleX = triple.first,
//        scaleY = triple.first,
//        translationX = triple.second,
//        translationY = triple.third
//      )
//      .pointerInput(deskAppMetaData) {
//        detectTransformGestures { centroid, pan, zoom, rotation ->
//          if (deskAppMetaData.screenType.value == DeskAppMetaData.ScreenType.Half) deskAppMetaData.zoom.value *= zoom
//        }
//      }
      .clickableWithNoEffect {
        localOpenList.remove(deskAppMetaData)
        localOpenList.add(deskAppMetaData)
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
//      deskAppMetaData.viewItem?.let { multiViewItem ->
//        MultiWebView(mmid = deskAppMetaData.mmid, viewItem = multiViewItem)
//      }
    }
    Box(modifier = Modifier
//      .pointerInput(deskAppMetaData) {
//        detectTransformGestures { centroid, pan, zoom, rotation ->
//          if (deskAppMetaData.screenType.value == DeskAppMetaData.ScreenType.Half) {
//            deskAppMetaData.offsetX.value += pan.x * deskAppMetaData.zoom.value
//            deskAppMetaData.offsetY.value += pan.y * deskAppMetaData.zoom.value
//          }
//        }
//      }
      .clickableWithNoEffect {
        if (localDrawerManager.visibleState.targetState) localDrawerManager.hide() else localDrawerManager.show()
      }) {
//      Toolbar(
//        deskAppMetaData = deskAppMetaData,
//        onClose = { localOpenList.remove(deskAppMetaData) },
//        onExpand = {
//          if (deskAppMetaData.screenType.value == DeskAppMetaData.ScreenType.Full) {
//            deskAppMetaData.screenType.value = DeskAppMetaData.ScreenType.Half
//            deskAppMetaData.isExpand = false
//          } else {
//            deskAppMetaData.screenType.value = DeskAppMetaData.ScreenType.Full
//            deskAppMetaData.isExpand = true
//          }
//        }
//      )
    }
  }
}

@Composable
internal fun Toolbar(
  deskAppMetaData: DeskAppMetaData,
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
      text = deskAppMetaData.name,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.Center,
      maxLines = 1
    )
//    Icon(
//      imageVector = ImageVector.vectorResource(
//        if (deskAppMetaData.screenType.value == DeskAppMetaData.ScreenType.Full) R.drawable.ic_shrink else R.drawable.ic_expand
//      ),
//      contentDescription = "FullScreen",
//      modifier = Modifier
//        .size(32.dp)
//        .clickableWithNoEffect { onExpand() }
//    )
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



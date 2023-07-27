package info.bagen.dwebbrowser.microService.browser.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.browser.desktop.DeskAppMetaData
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalDrawerManager
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalOpenList
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect

private val rightEnterAnimator = slideInHorizontally(animationSpec = tween(500),//动画时长1s
  initialOffsetX = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })
private val rightExitAnimator = slideOutHorizontally(animationSpec = tween(500),//动画时长1s
  targetOffsetX = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })

@Composable
internal fun DrawerView() {
  val localDrawerManager = LocalDrawerManager.current
  AnimatedVisibility(
    visibleState = localDrawerManager.visibleState,
    enter = rightEnterAnimator,
    exit = rightExitAnimator,
  ) {
    DrawerInnerView()
  }
}

@Composable
internal fun DrawerInnerView() {
  val localHeight = LocalConfiguration.current.screenHeightDp
  val localWidth = LocalConfiguration.current.screenWidthDp
  val localOpenList = LocalOpenList.current
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .width((localWidth * 0.3f - 32).dp)
        .height((localHeight * 0.7f).dp)
        .shadow(2.dp, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        .background(MaterialTheme.colorScheme.background)
        .align(Alignment.CenterEnd),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
      ) {
        itemsIndexed(localOpenList) { _, item ->
          AsyncImage(
            model = item.jsMetaData.icons.firstOrNull(),
            contentDescription = "Icon",
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickableWithNoEffect {
                item.screenType.value =
                  if (item.isExpand) DeskAppMetaData.ScreenType.Full else DeskAppMetaData.ScreenType.Half
                localOpenList.remove(item)
                localOpenList.add(item)
              }
          )
        }
      }

      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )

      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_main_home),
        contentDescription = "Home",
        modifier = Modifier
          .padding(8.dp)
          .clickableWithNoEffect {
            localOpenList.forEach { it.screenType.value = DeskAppMetaData.ScreenType.Hide }
          },
        tint = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Preview
@Composable
internal fun DrawerPreview() {
  DrawerView()
}
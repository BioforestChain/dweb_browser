package info.bagen.dwebbrowser.microService.browser.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.microService.browser.desktop.DeskAppMetaData
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalDrawerManager
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalInstallList
import info.bagen.dwebbrowser.microService.browser.desktop.model.LocalOpenList
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect

@Composable
fun DesktopMainView(mainView: (@Composable () -> Unit)? = null ,onClick: (DeskAppMetaData) -> Unit) {
  val installList = remember { mutableStateListOf<DeskAppMetaData>() }

  /*val screenWidth = LocalConfiguration.current.screenWidthDp
  val density = LocalDensity.current.density

  LaunchedEffect(installList) {
    withContext(ioAsyncExceptionHandler) {
      AppInfoDataStore.queryAppInfoList().collectLatest {
        it.forEach { (_, value) ->
          val jmmMetadata = gson.fromJson(value, JmmMetadata::class.java)
          DeskAppMetaData(jmmMetadata = jmmMetadata).also { appInfo ->
            appInfo.zoom.value = 0.7f
            appInfo.offsetX.value = (16 - screenWidth * 0.15f) * density
            appInfo.offsetY.value = 0f
            installList.add(appInfo)
          }
          installList.addAll(desktopAppList)
        }
      }
    }
  }*/

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. 主界面内容
    mainView?.let { it() } ?: MainView(onClick)
    // 2. 顶部的工具栏和显示网页是一个整体
    DesktopPager()
    // 3. 右边的工具栏
    DrawerView()
  }
}

@Composable
internal fun MainView(onClick: (DeskAppMetaData) -> Unit) {
  val installList = LocalInstallList.current
  val openList = LocalOpenList.current
  val localDrawerManager = LocalDrawerManager.current
  Box(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalArrangement = Arrangement.Center,
      contentPadding = PaddingValues(8.dp)
    ) {
      itemsIndexed(installList) { _, DeskAppMetaData ->
        MainAppItemView(DeskAppMetaData = DeskAppMetaData) {
          localDrawerManager.show()
          onClick(DeskAppMetaData)
          /*openList.find { it.jmmMetadata.id == item.jmmMetadata.id }?.let {
            // move to front
            openList.remove(item)
            openList.add(item)
            item.screenType.value = if (item.expand) AppInfo.ScreenType.Full else AppInfo.ScreenType.Half
          } ?: run {
            item.screenType.value = AppInfo.ScreenType.Half
            openList.add(item)
          }*/
        }
      }
    }
  }
}

@Composable
internal fun MainAppItemView(DeskAppMetaData: DeskAppMetaData, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .padding(8.dp)
      .clickableWithNoEffect { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    AsyncImage(
      model = DeskAppMetaData.jsMetaData.icons,
      contentDescription = DeskAppMetaData.jsMetaData.name
    )
    Text(text = DeskAppMetaData.jsMetaData.name, maxLines = 1)
  }
}
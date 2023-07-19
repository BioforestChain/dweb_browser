package info.bagen.dwebbrowser.microService.desktop.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.desktop.db.desktopAppList
import info.bagen.dwebbrowser.microService.desktop.model.AppInfo
import info.bagen.dwebbrowser.microService.desktop.model.LocalInstallList
import info.bagen.dwebbrowser.microService.desktop.model.LocalOpenList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.database.AppInfoDataStore
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.help.gson

@Preview
@Composable
fun DesktopMainView() {
  val installList = remember { mutableStateListOf<AppInfo>() }
  val openList = remember { mutableStateListOf<AppInfo>() }

  LaunchedEffect(installList) {
    withContext(ioAsyncExceptionHandler) {
      AppInfoDataStore.queryAppInfoList().collectLatest {
        it.forEach { (mmid, value) ->
          val jmmMetadata = gson.fromJson(value, JmmMetadata::class.java)
          installList.add(AppInfo(jmmMetadata = jmmMetadata))
          installList.addAll(desktopAppList)
        }
      }
    }
  }

  CompositionLocalProvider(
    LocalInstallList provides installList,
    LocalOpenList provides openList,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // 1. 主界面内容
      MainView()
      // 2. 顶部的工具栏和显示网页是一个整体
      DesktopPager()
      // 3. 右边的工具栏
      DrawerView()
    }
  }
}

@Composable
internal fun MainView() {
  val installList = LocalInstallList.current
  val openList = LocalOpenList.current
  Box(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalArrangement = Arrangement.Center,
      contentPadding = PaddingValues(8.dp)
    ) {
      itemsIndexed(installList) { _, item ->
        MainAppItemView(appInfo = item) {
          openList.find { it.jmmMetadata.id == item.jmmMetadata.id }?.let {
            // move to front
            openList.remove(item)
            openList.add(item)
            item.screenType.value = if (item.expand) AppInfo.ScreenType.Full else AppInfo.ScreenType.Half
          } ?: run {
            item.screenType.value = AppInfo.ScreenType.Half
            openList.add(item)
          }
        }
      }
    }
  }
}

@Composable
internal fun MainAppItemView(appInfo: AppInfo, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .padding(8.dp)
      .clickableWithNoEffect { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    AsyncImage(model = appInfo.jmmMetadata.icon, contentDescription = appInfo.jmmMetadata.name)
    Text(text = appInfo.jmmMetadata.name, maxLines = 1)
  }
}
package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.model.DesktopAppData
import org.dweb_browser.browser.desk.model.DesktopAppModel.DesktopAppRunStatus
import org.dweb_browser.browser.desk.toIntOffset
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.compose.clickableWithNoEffect

@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val appMenuPanel = rememberAppMenuPanel(desktopController, microModule)
  val scope = rememberCoroutineScope()

  val apps = rememberDesktopApps(desktopController)

  fun doOpen(mmid: String) {
    val index = apps.indexOfFirst {
      it.mmid == mmid
    }

    if (index == -1) {
      return
    }

    val oldApp = apps[index]
    when (oldApp.data) {
      is DesktopAppData.App -> {
        if (oldApp.running == DesktopAppRunStatus.NONE) {
          apps[index] = oldApp.copy(running = DesktopAppRunStatus.TORUNNING)
          desktopController.toRunningApps.add(mmid)
        }
        scope.launch {
          desktopController.open(mmid)
        }
      }

      is DesktopAppData.WebLink -> {
        scope.launch {
          desktopController.openWebLink(oldApp.data.url)
        }
      }
    }
  }


  val blurState by animateDpAsState(
    if (appMenuPanel.isOpenMenu) 20.dp else 0.dp, animationSpec = deskAniSpec()
  )

  val searchBar = rememberDesktopSearchBar()
  val desktopWallpaper = rememberDesktopWallpaper()
  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(Color.Black).blur(blurState),
    contentAlignment = Alignment.TopStart
  ) {
    desktopWallpaper.Render(Modifier.clickableWithNoEffect {
      searchBar.close()
      desktopWallpaper.play()
    })
    val density = LocalDensity.current.density
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeGestures)
        .padding(top = desktopTap())
        .onGloballyPositioned {
          val pos = it.positionInWindow().toIntOffset(density)
          appMenuPanel.safeAreaInsets = WindowInsets(
            top = pos.y,
            left = pos.x,
            right = ((maxWidth.value * density) - pos.x - it.size.width).toInt(),
            bottom = 0
          )
        }
    ) {
      searchBar.Render(
        Modifier.windowInsetsPadding(WindowInsets.safeGestures.only(WindowInsetsSides.Top))
      )
      LaunchedEffect(Unit) {
        searchBar.onSearchFlow.collect {
          desktopController.search(it)
        }
      }

      val layout = desktopGridLayout()
      LazyVerticalGrid(
        columns = layout.cells,
        contentPadding = PaddingValues(layout.contentPadding),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.horizontalSpace),
        verticalArrangement = Arrangement.spacedBy(layout.verticalSpace)
      ) {
        itemsIndexed(apps) { index, app ->
          AppItem(
            modifier = Modifier.desktopAppItemActions(onOpenApp = {
              doOpen(app.mmid)
              searchBar.close()
              desktopWallpaper.play()
            }, onOpenAppMenu = {
              apps.getOrNull(index)?.also {
                appMenuPanel.show(it)
              }
            }),
            app,
            microModule,
          )
        }
      }
    }
  }

  appMenuPanel.Render()
}


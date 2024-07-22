package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.model.DesktopAppData
import org.dweb_browser.browser.desk.model.DesktopAppModel.DesktopAppRunStatus
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.compose.clickableWithNoEffect

@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
  modifier: Modifier = Modifier,
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

  val searchBar = rememberDesktopSearchBar()
  val desktopWallpaper = rememberDesktopWallpaper()
  Box(modifier) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().then(
        when {
          canSupportModifierBlur() -> Modifier.blur(20.dp * appMenuPanel.visibilityProgress)
          else -> Modifier
        }
      ),
      contentAlignment = Alignment.TopStart,
    ) {
      desktopWallpaper.Render(Modifier.clickableWithNoEffect {
        if (searchBar.isOpened) {
          searchBar.close()
        } else {
          desktopWallpaper.play()
        }
      })
      val layout = desktopGridLayout()
      val containerPadding = WindowInsets.safeGestures
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(containerPadding)
          .padding(top = desktopTap())
      ) {
        searchBar.Render(Modifier.padding(vertical = 16.dp))
        LaunchedEffect(Unit) {
          searchBar.onSearchFlow.collect {
            desktopController.search(it)
          }
        }
        val innerInsets = layout.insets.exclude(WindowInsets.safeGestures)

        LazyVerticalGrid(
          columns = layout.cells,
          contentPadding = innerInsets.asPaddingValues(),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp).onGloballyPositioned {
            val pos = it.positionInWindow()//.toIntOffset(d)
            appMenuPanel.safeAreaInsets = WindowInsets(
              top = pos.y.toInt(),
              left = pos.x.toInt(),
              right = pos.x.toInt(),//((maxWidth.value * d) - pos.x - it.size.width).toInt(),
              bottom = 0
            )
          },
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

    appMenuPanel.Render(Modifier.matchParentSize())
  }
}

package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopController
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

  val apps = desktopController.appsFlow.collectAsState().value

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
          desktopWallpaper.playJob()
        }
      })
      val layout = desktopGridLayout()
      val layoutDirection = LocalLayoutDirection.current
      val density = LocalDensity.current

      val safeGestures = WindowInsets.safeGestures
      val outerPadding = remember(layout.insets, safeGestures) {
        val desktopPadding = layout.insets.union(safeGestures).asPaddingValues(density)
        val safeGesturesPadding = safeGestures.asPaddingValues(density)
        PaddingValues(
          top = desktopPadding.calculateTopPadding(),
          start = safeGesturesPadding.calculateStartPadding(layoutDirection),
          bottom = desktopPadding.calculateBottomPadding(),
          end = safeGesturesPadding.calculateEndPadding(layoutDirection),
        )
      }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(outerPadding).clickableWithNoEffect {
          searchBar.close()
          desktopWallpaper.playJob()
        }
      ) {
        searchBar.Render(Modifier.padding(vertical = 16.dp))
        LaunchedEffect(Unit) {
          searchBar.onSearchFlow.collect {
            desktopController.search(it)
          }
        }
        val innerPadding = remember(layout.insets, safeGestures) {
          with(layout.insets.exclude(safeGestures).asPaddingValues(density)) {
            PaddingValues(
              start = calculateStartPadding(layoutDirection),
              end = calculateEndPadding(layoutDirection),
            )
          }
        }
        LazyVerticalGrid(
          columns = layout.cells,
          contentPadding = innerPadding,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp).onGloballyPositioned {
            val pos = it.positionInWindow()
            appMenuPanel.safeAreaInsets = WindowInsets(
              top = pos.y.fastRoundToInt(),
              left = pos.x.fastRoundToInt(),
              right = pos.x.fastRoundToInt(),
              bottom = 0,
            )
          },
          horizontalArrangement = Arrangement.spacedBy(layout.horizontalSpace),
          verticalArrangement = Arrangement.spacedBy(layout.verticalSpace)
        ) {
          itemsIndexed(apps) { index, app ->
            AppItem(
              app = app,
              microModule = microModule,
              modifier = Modifier.desktopAppItemActions(
                onOpenApp = {
                  scope.launch {
                    desktopController.open(app.mmid)
                  }
//                  searchBar.close()
//                  desktopWallpaper.play()
                },
                onOpenAppMenu = {
                  apps.getOrNull(index)?.also {
                    appMenuPanel.show(it)
                  }
                },
              ),
            )
          }
        }
      }
    }

    appMenuPanel.Render(Modifier.matchParentSize())
  }
}

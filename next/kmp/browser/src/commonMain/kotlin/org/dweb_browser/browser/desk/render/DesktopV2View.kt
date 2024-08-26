package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.dweb_browser.browser.desk.DesktopV2Controller
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.mkdesklayout.project.DeskLayoutV6

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopV2Controller.RenderImpl() {

  val desktopController = this
  val microModule = deskNMM
  val appMenuPanel = rememberAppMenuPanel(desktopController, microModule)
  val scope = rememberCoroutineScope()

  val apps = desktopController.appsFlow.collectAsState().value
  val appsLayouts = desktopController.appLayoutsFlow.collectAsState().value

  val searchBar = rememberDesktopSearchBar()
  val desktopWallpaper = rememberDesktopWallpaper()
  Box(Modifier.fillMaxSize()) {
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().then(
        when {
          canSupportModifierBlur() -> Modifier.blur(20.dp * appMenuPanel.visibilityProgress)
          else -> Modifier
        }
      ),
      contentAlignment = Alignment.TopStart,
    ) {
      var edit by remember { mutableStateOf(false) }

      desktopWallpaper.Render(Modifier.clickableWithNoEffect {
        if (searchBar.isOpened) {
          searchBar.close()
        } else {
          desktopWallpaper.play()
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
        modifier = Modifier
          .fillMaxWidth()
          .padding(outerPadding)
          .combinedClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = {
              if (searchBar.isOpened) {
                searchBar.close()
              }
              if (edit) {
                edit = false
              } else {
                desktopWallpaper.play()
              }
            },
            onLongClick = {
              edit = true
            }
          ),
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
        key(apps, appsLayouts) {
          DeskLayoutV6(
            datas = apps,
            modifier = Modifier.fillMaxSize().padding(top = 4.dp).onGloballyPositioned {
              val pos = it.positionInWindow()
              appMenuPanel.safeAreaInsets = WindowInsets(
                top = pos.y.fastRoundToInt(),
                left = pos.x.fastRoundToInt(),
                right = pos.x.fastRoundToInt(),
                bottom = 0,
              )
            },
            edit = edit,
            contentPadding = innerPadding,
            layout = { screen ->
              appsLayouts.firstOrNull {
                it.screenWidth == screen
              }?.layouts?.mapKeys { entry ->
                apps.first {
                  it.mmid == entry.key
                }
              } ?: emptyMap()
            },
            relayout = { layoutScreenWidth, geoMaps ->
              scope.launch {
                desktopController.updateAppsLayouts(
                  screenWidth = layoutScreenWidth,
                  geoMaps.mapKeys { it.key.mmid })
              }
            }) { app, geometry, draging ->

            val iConModifier = Modifier.run {
              if (edit) {
                this
              } else {
                desktopAppItemActions(
                  onOpenApp = {
                    scope.launch {
                      desktopController.openAppOrActivate(app.mmid)
                    }
                  },
                  onOpenAppMenu = {
                    appMenuPanel.show(app)
                  },
                )
              }
            }

            AppItem(
              app = app,
              edit,
              draging,
              microModule = microModule,
              modifier = Modifier.fillMaxSize(),
              iconModifier = iConModifier
            )
          }
        }
      }
    }

    appMenuPanel.Render(Modifier.fillMaxSize())
  }
}

//LazyVerticalGrid(
//columns = layout.cells,
//contentPadding = innerPadding,
//modifier = Modifier.fillMaxWidth().padding(top = 4.dp).onGloballyPositioned {
//  val pos = it.positionInWindow()
//  appMenuPanel.safeAreaInsets = WindowInsets(
//    top = pos.y.fastRoundToInt(),
//    left = pos.x.fastRoundToInt(),
//    right = pos.x.fastRoundToInt(),
//    bottom = 0,
//  )
//},
//horizontalArrangement = Arrangement.spacedBy(layout.horizontalSpace),
//verticalArrangement = Arrangement.spacedBy(layout.verticalSpace)
//) {
//  itemsIndexed(apps) { index, app ->
//
//
//
//    AppItem(
//      app = app,
//      microModule = microModule,
//      edit = false,
//      editDragging = false,
//      modifier = Modifier.fillMaxSize(),
//      iconModifier = Modifier.run {
//        if (edit) {
//          this
//        } else {
//          desktopAppItemActions(
//            onOpenApp = {
//              scope.launch {
//                desktopController.openAppOrActivate(app.mmid)
//              }
//            },
//            onOpenAppMenu = {
//              appMenuPanel.show(app)
//            },
//          )
//        }
//      }
//    )
//  }
//}
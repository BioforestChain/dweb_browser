package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.model.DesktopAppData
import org.dweb_browser.browser.desk.model.DesktopAppModel.DesktopAppRunStatus
import org.dweb_browser.browser.desk.toIntOffset
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.compose.clickableWithNoEffect
import kotlin.math.max

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

  val searchBar = rememberDesktopSearchBar()
  val desktopWallpaper = rememberDesktopWallpaper()
  val graphicsLayer = rememberGraphicsLayer()
  var drawCacheSize by remember { mutableStateOf(IntSize.Zero) }
  var drawCacheImage by remember { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(desktopWallpaper){
    desktopWallpaper.onAnimationStopFlow.collect{
      println("QAQ onAnimationStopFlow")
      drawCacheImage = graphicsLayer.toImageBitmap()
    }
  }
  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().drawWithContent {
      // call record to capture the content in the graphics layer
      graphicsLayer.record {
        // draw the contents of the composable into the graphics layer
        this@drawWithContent.drawContent()
      }
      // draw the graphics layer on the visible canvas
      drawLayer(graphicsLayer)
    }.onGloballyPositioned {
      if (drawCacheSize != it.size) {
        drawCacheSize = it.size
        drawCacheImage = null
      }
    },
    contentAlignment = Alignment.TopStart
  ) {
    desktopWallpaper.Render(Modifier.clickableWithNoEffect {
      searchBar.close()
      desktopWallpaper.play()
    })
    val density = LocalDensity.current
    val d = density.density
    val layout = desktopGridLayout()
    val containerPadding = WindowInsets.safeGestures
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().windowInsetsPadding(containerPadding)
        .padding(top = desktopTap())
        .onGloballyPositioned {
          val pos = it.positionInWindow().toIntOffset(d)
          appMenuPanel.safeAreaInsets = WindowInsets(
            top = pos.y,
            left = pos.x,
            right = pos.x,//((maxWidth.value * d) - pos.x - it.size.width).toInt(),
            bottom = 0
          )
        }
    ) {
      searchBar.Render(Modifier.padding(vertical = 16.dp))
      LaunchedEffect(Unit) {
        searchBar.onSearchFlow.collect {
          desktopController.search(it)
        }
      }
      val innerPadding =
        layout.contentPadding.value - (containerPadding.getLeft(density, LayoutDirection.Ltr) / d)

      LazyVerticalGrid(
        columns = layout.cells,
        contentPadding = PaddingValues(max(0f, innerPadding).dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
  if (appMenuPanel.isOpenMenu || appMenuPanel.isOpenDeleteDialog) {
    val bg = produceState(drawCacheImage) {
      value = graphicsLayer.toImageBitmap().also { drawCacheImage = it }
    }.value
    bg?.also {
      val blurState by animateDpAsState(
        if (appMenuPanel.isOpenMenu) 20.dp else 0.dp, animationSpec = deskAniSpec()
      )
      Image(bg, null, Modifier.fillMaxSize().zIndex(2f).blur(blurState))
    }
  }

  appMenuPanel.Render(Modifier.zIndex(3f))
}

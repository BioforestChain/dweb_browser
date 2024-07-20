package org.dweb_browser.browser.desk.render

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.desktopWallpaperView
import org.dweb_browser.browser.desk.model.AppMenuType
import org.dweb_browser.browser.desk.model.DesktopAppData
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.model.DesktopAppModel.DesktopAppRunStatus
import org.dweb_browser.browser.desk.model.createAppMenuDisplays
import org.dweb_browser.browser.desk.toIntOffset
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.iosTween

@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
) {

  var popUpApp by remember { mutableStateOf<DesktopAppModel?>(null) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current


  var textWord by remember { mutableStateOf("") }

  val apps = rememberDesktopApps(desktopController)

  fun doHideKeyboard() {
    keyboardController?.hide()
    focusManager.clearFocus()
    textWord = ""
  }

  fun doSearch(words: String) {
    scope.launch {
      desktopController.search(words)
    }
  }

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

  fun doQuit(mmid: String) {
    desktopController.toRunningApps.remove(mmid)
    scope.launch {
      desktopController.quit(mmid)
    }
  }

  fun doDetail(mmid: String) {
    scope.launch {
      desktopController.detail(mmid)
    }
  }

  fun doUninstall(mmid: String) {
    scope.launch {
      desktopController.uninstall(mmid)
    }
  }


  fun doWebLinkDelete(mmid: String) {
    scope.launch {
      desktopController.removeWebLink(mmid)
    }
  }

  fun doShare(mmid: String) {
    scope.launch {
      desktopController.share(mmid)
    }
  }

  fun doHaptics() {
    scope.launch {
      microModule.nativeFetch("file://haptics.sys.dweb/vibrateHeavyClick")
    }
  }

  fun doShowPopUp(index: Int) {
    popUpApp = apps.getOrNull(index)?.also {
      doHaptics()
      showMoreMenu = true
    }
  }

  fun doHidePopUp() {
    showMoreMenu = false
    showDeleteDialog = false
    popUpApp = null
  }

  val blurState by animateDpAsState(
    if (popUpApp != null) 20.dp else 0.dp, animationSpec = iosTween(popUpApp != null)
  )

  val channel = remember { Channel<Unit>() }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(Color.Black).blur(blurState),
    contentAlignment = Alignment.TopStart
  ) {
    desktopWallpaperView(
      desktopBgCircleCount(), modifier = Modifier, isTapDoAnimation = true, channel.receiveAsFlow()
    ) {
      doHideKeyboard()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeGestures)
        .padding(top = desktopTap()).clickableWithNoEffect {
          doHideKeyboard()
          scope.launch {
            channel.send(Unit)
          }
        }) {

      DesktopSearchBar(
        textWord,
        ::doSearch,
//        ::doHideKeyboard,
        Modifier.windowInsetsPadding(WindowInsets.safeGestures),
      )

      val layout = desktopGridLayout()
      LazyVerticalGrid(
        columns = layout.cells,
        contentPadding = PaddingValues(layout.contentPadding),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.space),
        verticalArrangement = Arrangement.spacedBy(layout.space)
      ) {
        itemsIndexed(apps) { index, app ->
          AppItem(
            modifier = Modifier.desktopAppItemActions(onTap = {
              doOpen(app.mmid)
            }, onMenu = {
              doShowPopUp(index)
            }),
            app,
            microModule,
          )
        }
      }
    }
  }

  val iconScale by animateFloatAsState(
    if (popUpApp != null) 1.05f else 1f, animationSpec = iosTween(popUpApp != null)
  )
  val iconContainerAlpha by animateFloatAsState(
    if (popUpApp != null) 0.8f else 0.2f, animationSpec = iosTween(popUpApp != null)
  )

  if (showMoreMenu) {
    popUpApp?.also { app ->
      BoxWithConstraints(
        Modifier.zIndex(2f).fillMaxSize().clickableWithNoEffect {
          doHidePopUp()
        }, Alignment.TopStart
      ) {

        DeskAppIcon(
          app,
          microModule,
          containerAlpha = iconContainerAlpha,
          modifier = Modifier.requiredSize(app.size.width.dp, app.size.height.dp).offset {
            app.offSet.toIntOffset(1f)
          }.scale(iconScale),
        )

        val moreAppDisplayModels by remember(app) {
          mutableStateOf(createAppMenuDisplays(app))
        }

        val itemSize = IntSize(60, 60)
        val moreAppDisplayMenuWidth = moreAppDisplayModels.size * itemSize.width
        val moreAppDisplayMenuHeight = itemSize.height
        val density = LocalDensity.current.density

        val moreAppDisplayOffSet by remember(app.offSet, moreAppDisplayModels) {
          //此处有坑：在于app.offset, app.size, 取到的数值倍数不一致。。。
          val minX = app.offSet.x.toInt()
          val maxY = app.offSet.y.toInt() + app.size.height * density
          val minY = app.offSet.y.toInt()
          val space = 5
          mutableStateOf(
            IntOffset(
              if (minX + moreAppDisplayMenuWidth * density < constraints.maxWidth) minX else constraints.maxWidth - space - (moreAppDisplayMenuWidth * density).toInt(),
              if (maxY + space + moreAppDisplayMenuHeight * density < constraints.maxHeight) maxY.toInt() + space else minY - space - (moreAppDisplayMenuHeight * density).toInt(),
            )
          )
        }

        AppMoreRender(moreAppDisplayModels,
          Modifier.offset { moreAppDisplayOffSet }
            .size(moreAppDisplayMenuWidth.dp, moreAppDisplayMenuHeight.dp),
          action = { type ->
            when (type) {
              AppMenuType.OFF -> doHidePopUp().also { doQuit(app.mmid) }
              AppMenuType.DETAIL -> doHidePopUp().also { doDetail(app.mmid) }
              AppMenuType.SHARE -> doHidePopUp().also { doShare(app.mmid) }
              AppMenuType.UNINSTALL, AppMenuType.DELETE -> {
                showMoreMenu = false
                showDeleteDialog = true
              }
            }
          })
      }
    }
  }

  AnimatedVisibility(showDeleteDialog) {
    popUpApp?.let { app ->
      DeleteAlert(app, microModule, onDismissRequest = ::doHidePopUp, confirm = {
        doHidePopUp().also {
          when (app.data) {
            is DesktopAppData.App -> doUninstall(app.mmid)
            is DesktopAppData.WebLink -> doWebLinkDelete(app.mmid)
          }
        }
      })
    }
  }
}

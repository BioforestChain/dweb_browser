package org.dweb_browser.browser.desk.render

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import org.dweb_browser.browser.desk.model.getAppMenuDisplays
import org.dweb_browser.browser.desk.toIntOffset
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.compose.clickableWithNoEffect

@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  /**
   * 是否打开应用菜单
   */
//  var isOpenMenu by remember { mutableStateOf(false) }
  var openMenuApp by remember { mutableStateOf<DesktopAppModel?>(null) }
  val appMenuPanel = openMenuApp?.let { rememberAppMenuPanel(it, desktopController, microModule) }
  val isOpenMenu = appMenuPanel?.isOpenMenu == true

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
    if (isOpenMenu) 20.dp else 0.dp, animationSpec = deskAniSpec()
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

      val searchBar = rememberDesktopSearchBar()
      searchBar.Render(Modifier.windowInsetsTopHeight(WindowInsets.safeGestures))
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
            }, onOpenAppMenu = {
              openMenuApp = apps.getOrNull(index)
            }),
            app,
            microModule,
          )
        }
      }
    }
  }

  appMenuPanel?.Render()
}

@Composable
internal fun rememberAppMenuPanel(
  app: DesktopAppModel, desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime
) = remember { AppMenuPanel(app, desktopController, microModule) }.also { it.app = app }

internal class AppMenuPanel(
  app: DesktopAppModel,
  val desktopController: DesktopController,
  val microModule: NativeMicroModule.NativeRuntime
) {
  var app by mutableStateOf(app)
  val displays = app.getAppMenuDisplays()
  var isClose by mutableStateOf(false)
  var isOpenMenu by mutableStateOf(false)
  val menuProgressAni = Animatable(0f)
  var isOpenDeleteDialog by mutableStateOf(false)
  fun hide() {
    isOpenMenu = false
    isOpenDeleteDialog = false
  }


  fun show(app: DesktopAppModel) {
    this.app = app
    doHaptics()
    isOpenMenu = true
  }


  @Composable
  fun Render() {
//    val isOpenMenuState = MutableTransitionState<Boolean>(false)
//    AnimatedVisibility(isOpenMenuState, enter = expandIn()) {
//
//    }
    /**
     * 图层是否最终可见
     */
    var moreMenuVisibility by remember { mutableStateOf(false) }
    LaunchedEffect(isOpenMenu) {
      moreMenuVisibility = true
      if (isOpenMenu) {
        menuProgressAni.animateTo(1f, deskAniSpec())
      } else {
        menuProgressAni.animateTo(0f, deskAniSpec())
        moreMenuVisibility = false
      }
    }
    if (moreMenuVisibility) {
      NativeBackHandler(isOpenMenu) {
        hide()
      }
    }
    BoxWithConstraints(
      Modifier.zIndex(2f).fillMaxSize().clickableWithNoEffect(isClose) { hide() },
      Alignment.TopStart,
    ) {
      val iconScale = 1f + 0.1f * menuProgressAni.value
      DeskAppIcon(
        app, microModule,
        modifier = Modifier.requiredSize(app.size.width.dp, app.size.height.dp).offset {
          app.offset.toIntOffset(1f)
        }.scale(iconScale),
      )

      val itemSize = IntSize(60, 60)
      val moreAppDisplayMenuWidth = displays.size * itemSize.width
      val moreAppDisplayMenuHeight = itemSize.height
      val density = LocalDensity.current.density

      val moreAppDisplayOffSet by remember(app.offset, displays) {
        //此处有坑：在于app.offset, app.size, 取到的数值倍数不一致。。。
        val minX = app.offset.x.toInt()
        val maxY = app.offset.y.toInt() + app.size.height * iconScale * density
        val minY = app.offset.y.toInt()
        val space = 8
        mutableStateOf(
          IntOffset(
            if (minX + moreAppDisplayMenuWidth * density < constraints.maxWidth) minX else constraints.maxWidth - space - (moreAppDisplayMenuWidth * density).toInt(),
            if (maxY + space + moreAppDisplayMenuHeight * density < constraints.maxHeight) maxY.toInt() + space else minY - space - (moreAppDisplayMenuHeight * density).toInt(),
          )
        )
      }

      val menuAlpha = menuProgressAni.value
      val menuScale = 0.5f + 0.5f * menuProgressAni.value
      AppMenu(displays,
        Modifier.offset { moreAppDisplayOffSet }
          .size(moreAppDisplayMenuWidth.dp, moreAppDisplayMenuHeight.dp)
          .alpha(menuAlpha),
        action = { type ->
          when (type) {
            AppMenuType.OFF -> hide().also { doQuit(app.mmid) }
            AppMenuType.DETAIL -> hide().also { doDetail(app.mmid) }
            AppMenuType.SHARE -> hide().also { doShare(app.mmid) }
            AppMenuType.UNINSTALL, AppMenuType.DELETE -> {
              isOpenMenu = false
              isOpenDeleteDialog = true
            }
          }
        })
    }

    AnimatedVisibility(isOpenDeleteDialog) {
      DeleteAlert(app, microModule, onDismissRequest = { hide() }, onConfirm = {
        hide()
        when (app.data) {
          is DesktopAppData.App -> doUninstall(app.mmid)
          is DesktopAppData.WebLink -> doWebLinkDelete(app.mmid)
        }
      })
    }
  }

  fun doQuit(mmid: String) {
    desktopController.toRunningApps.remove(mmid)
    microModule.scopeLaunch(cancelable = true) {
      desktopController.quit(mmid)
    }
  }

  fun doDetail(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.detail(mmid)
    }
  }

  fun doUninstall(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.uninstall(mmid)
    }
  }


  fun doWebLinkDelete(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.removeWebLink(mmid)
    }
  }

  fun doShare(mmid: String) {
    microModule.scopeLaunch(cancelable = true) {
      desktopController.share(mmid)
    }
  }

  fun doHaptics() {
    microModule.scopeLaunch(cancelable = true) {
      microModule.nativeFetch("file://haptics.sys.dweb/vibrateHeavyClick")
    }
  }
}
package org.dweb_browser.browser.desk

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.desk.DesktopAppModel.DesktopAppRunStatus
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.div
import org.dweb_browser.helper.compose.iosTween
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.image.compose.ImageLoadResult
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.blobFetchHook

@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val apps = remember { mutableStateListOf<DesktopAppModel>() }

  var popUpApp by remember { mutableStateOf<DesktopAppModel?>(null) }

  val scope = rememberCoroutineScope()

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val toRunningApps by remember { mutableStateOf(mutableSetOf<String>()) }

  var textWord by remember { mutableStateOf("") }

  fun doGetApps() {
    scope.launch {
      val installApps = desktopController.getDesktopApps().map {
        val icon = it.icons.toStrict().pickLargest()
        val isSystemApp = desktopController.isSystemApp(it.mmid)
        //TODO: 临时的处理。等待分拆weblink后再优化。
        val isWebLink =
          it.categories.contains(MICRO_MODULE_CATEGORY.Web_Browser) && it.mmid != "web.browser.dweb"

        val runStatus = if (it.running) {
          toRunningApps.remove(it.mmid)
          DesktopAppRunStatus.RUNNING
        } else if (toRunningApps.contains(it.mmid)) {
          DesktopAppRunStatus.TORUNNING
        } else {
          DesktopAppRunStatus.NONE
        }

        val oldApp = apps.find { oldApp ->
          oldApp.mmid == it.mmid
        }

        oldApp?.copy(running = runStatus) ?: DesktopAppModel(
          it.short_name.ifEmpty { it.name },
          it.mmid,
          if (isWebLink) DesktopAppData.WebLink(mmid = it.mmid, url = it.name) else DesktopAppData.App(mmid = it.mmid),
          icon,
          isSystemApp,
          runStatus,
        )
      }
      apps.clear()
      apps.addAll(installApps)
    }
  }

  DisposableEffect(Unit) {
    val job = desktopController.onUpdate.run {
      filter { it != "bounds" }
    }.collectIn(scope) {
      doGetApps()
    }

    onDispose {
      job.cancel()
    }
  }

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
          toRunningApps.add(mmid)
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
    toRunningApps.remove(mmid)
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

  fun doDelete(mmid: String) {
    //TODO: 后期优化
    doUninstall(mmid)
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
    }
  }

  fun doHidePopUp() {
    popUpApp = null
  }

  val blurState by animateDpAsState(
    if (popUpApp != null) 3.dp else 0.dp, animationSpec = iosTween(popUpApp != null)
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

      desktopSearchBar(
        textWord,
        Modifier.windowInsetsPadding(WindowInsets.safeGestures),
        ::doSearch,
        ::doHideKeyboard
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
            modifier = Modifier.DesktopEventDetector(onClick = {
              doOpen(app.mmid)
            }, onDoubleClick = {}, onLongClick = {
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
        mutableStateOf(createMoreAppDisplayModels(app))
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

      moreAppItemsDisplay(moreAppDisplayModels,
        Modifier.offset { moreAppDisplayOffSet }
          .size(moreAppDisplayMenuWidth.dp, moreAppDisplayMenuHeight.dp),
        dismiss = { doHidePopUp() },
        action = { type ->
          when (type) {
            MoreAppModelType.OFF -> doQuit(app.mmid)
            MoreAppModelType.DETAIL -> doDetail(app.mmid)
            MoreAppModelType.UNINSTALL -> doUninstall(app.mmid)
            MoreAppModelType.DELETE -> doDelete(app.mmid)
            MoreAppModelType.SHARE -> doShare(app.mmid)
          }
        })
    }
  }
}

private typealias AppItemAction = (String) -> Unit

@Composable
private fun AppItem(
  modifier: Modifier,
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val density = LocalDensity.current.density
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    DeskAppIcon(app, microModule, modifier = Modifier.onGloballyPositioned {
      app.size = it.size / density
      app.offSet = it.positionInWindow()
    }.jump(app.running == DesktopAppRunStatus.TORUNNING))
    Text(
      text = app.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        shadow = Shadow(Color.Black, Offset(4f, 4f), 4f)
      ), modifier = Modifier.fillMaxWidth()
    )
  }
}

fun Modifier.jump(enable: Boolean) = this.composed {
  var animated by remember { mutableStateOf(false) }
  var animationCount by remember { mutableStateOf(Pair(1, false)) }
  val animationDur = 300
  var offValue = remember { Animatable(0f) }

  LaunchedEffect(enable) {
    if (enable) {
      animationCount = Pair(0, false)
      animated = true

      launch {
        while (true) {
          offValue.animateTo(-8f, tween(durationMillis = animationDur))
          animationCount = animationCount.copy(animationCount.first, true)
          offValue.animateTo(0f, tween(durationMillis = animationDur))
          animationCount = animationCount.copy(animationCount.first + 1, false)
        }
      }
    } else if (animationCount.first < 1) {
      animated = true
      launch {
        if (!animationCount.second) {
          val time = (-300 * offValue.value / 8f).toInt()
          offValue.animateTo(-8f, tween(time))
        }
        val time = 300 - (300 * offValue.value / 8f).toInt()
        offValue.animateTo(0f, tween(time))
        animated = false
      }
    } else {
      animated = false
    }
  }

  this.offset(0.dp, offValue.value.dp)
}

@Composable
private fun DeskAppIcon(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  containerAlpha: Float? = null,
  modifier: Modifier = Modifier
) {
  val iconSize = desktopIconSize()
  DeskCacheIcon(
    app.icon,
    microModule,
    iconSize.width.dp,
    iconSize.height.dp,
    containerAlpha = containerAlpha,
    modifier.padding(8.dp)
  )
}

private data class MoreAppModel(val type: MoreAppModelType, val enable: Boolean)

private enum class MoreAppModelType {
  OFF {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.quit.text, Icons.Outlined.HighlightOff
      )
  },

  DETAIL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.detail.text, Icons.Outlined.Description
      )
  },

  UNINSTALL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(
        BrowserI18nResource.Desktop.uninstall.text, Icons.Outlined.Delete
      )
  },

  SHARE {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(BrowserI18nResource.Desktop.share.text, Icons.Outlined.Share)
  },

  DELETE {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData(BrowserI18nResource.Desktop.delete.text, Icons.Outlined.Delete)
  };

  data class MoreAppModelTypeData(val title: String, val icon: ImageVector)

  abstract val data: MoreAppModelTypeData
}

@Composable
private fun moreAppItemsDisplay(
  displays: List<MoreAppModel>,
  modifier: Modifier,
  action: (MoreAppModelType) -> Unit,
  dismiss: () -> Unit,
) {
  Row(
    modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.5f)),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    displays.forEach {
      Column(
        modifier = Modifier.padding(4.dp).fillMaxSize().weight(1f).clickable(enabled = it.enable) {
          action(it.type)
          dismiss()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Icon(
          imageVector = it.type.data.icon,
          contentDescription = null,
          tint = if (it.enable) Color.Black else Color.Gray
        )
        Text(
          it.type.data.title,
          Modifier,
          fontSize = 12.sp,
          color = if (it.enable) Color.Black else Color.Gray
        )
      }
    }
  }
}

data class DesktopGridLayout(val cells: GridCells, val contentPadding: Dp, val space: Dp)

expect fun desktopGridLayout(): DesktopGridLayout

expect fun desktopTap(): Dp
expect fun desktopBgCircleCount(): Int
expect fun desktopIconSize(): IntSize

@Composable
expect fun Modifier.DesktopEventDetector(
  onClick: () -> Unit, onDoubleClick: () -> Unit, onLongClick: () -> Unit
): Modifier

private fun createMoreAppDisplayModels(
  app: DesktopAppModel
): List<MoreAppModel> {

  val displays = mutableListOf<MoreAppModel>()

  when (app.data) {
    is DesktopAppData.App -> {
      displays.add(
        MoreAppModel(MoreAppModelType.OFF, app.running == DesktopAppRunStatus.RUNNING)
      )
      if (!app.isSystermApp) {
        displays.add(MoreAppModel(MoreAppModelType.DETAIL, true))
        displays.add(MoreAppModel(MoreAppModelType.UNINSTALL, true))
      }
    }

    is DesktopAppData.WebLink -> {
      displays.add(MoreAppModel(MoreAppModelType.DELETE, true))
    }
  }

  displays.add(MoreAppModel(MoreAppModelType.SHARE, false))

  return displays
}

@Composable
fun desktopSearchBar(
  textWord: String, modifier: Modifier, search: (String) -> Unit, hideKeyBoad: () -> Unit
) {

  val searchWord = remember {
    mutableStateOf(TextFieldValue(textWord))
  }

  searchWord.value = TextFieldValue(textWord)

  val searchBarWR = 0.9F
  var expand by remember { mutableStateOf(0F) }
  val transition = updateTransition(expand, "search bar expand")
  val searchIconOffset = transition.animateIntOffset {
    if (it > 0) {
      val density = LocalDensity.current.density
      //30指的是size = 30.dp, 这边需要重新将dp转为px
      IntOffset(-(it * searchBarWR / 2.0 - 30 * density / 2.0 - 5).toInt(), 0)
    } else {
      IntOffset.Zero
    }
  }
  val searchBarWidthRadio = transition.animateFloat {
    if (it > 0) {
      searchBarWR
    } else {
      0.5F
    }
  }

  fun doSearchTirggleAnimation(on: Boolean, searchTextFieldWidth: Int) {
    expand = if (on) searchTextFieldWidth.toFloat() else 0F
  }

  BoxWithConstraints(modifier = modifier.padding(10.dp)) {
    BasicTextField(
      searchWord.value,
      onValueChange = {
        searchWord.value = it
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = {
        hideKeyBoad()
        search(searchWord.value.text)
        searchWord.value = TextFieldValue("")
      }),
      decorationBox = {
        Box(
          contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()
        ) {

          Icon(imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp).offset {
              searchIconOffset.value
            })

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)
          ) {
            Box(modifier = Modifier.fillMaxWidth()) {
              it()
            }
          }
        }
      },
      textStyle = TextStyle(Color.White, fontSize = TextUnit(18f, TextUnitType.Sp)),
      cursorBrush = SolidColor(Color.White),
      modifier = Modifier.onFocusChanged {
        doSearchTirggleAnimation(it.isFocused, constraints.maxWidth)
      }.fillMaxWidth(searchBarWidthRadio.value).height(44.dp).background(color = Color.Transparent)
        .clip(RoundedCornerShape(8.dp))
        .border(1.dp, color = Color.White, shape = RoundedCornerShape(22.dp))
        .windowInsetsPadding(WindowInsets.safeGestures)
    )
  }
}

private sealed class DesktopAppData {
  abstract val mmid: String
  data class App(override val mmid: String) : DesktopAppData()
  data class WebLink(override val mmid: String, val url: String) : DesktopAppData()
}

private data class DesktopAppModel(
  val name: String,
  val mmid: MMID,
  val data: DesktopAppData,
  val icon: StrictImageResource?,
  val isSystermApp: Boolean,
  var running: DesktopAppRunStatus = DesktopAppRunStatus.NONE,
  var image: ImageLoadResult? = null,
  val id: UUID = randomUUID()
) {

  var size: Size by mutableStateOf(Size.Zero)
  var offSet: Offset by mutableStateOf(Offset.Zero)

  enum class DesktopAppRunStatus {
    NONE, TORUNNING, RUNNING
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as DesktopAppModel

    if (mmid != other.mmid) return false
    if (name != other.name) return false
    if (running != other.running) return false

    return true
  }


  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + mmid.hashCode() + 100 * running.hashCode()
    return result
  }

  override fun toString(): String {
    return "${id}, ${mmid}, ${running}"
  }
}

@Composable
fun DeskCacheIcon(
  icon: StrictImageResource?,
  microModule: NativeMicroModule.NativeRuntime,
  width: Dp,
  height: Dp,
  containerAlpha: Float? = null,
  modifier: Modifier = Modifier,
) {
  val imageResult =
    icon?.let { PureImageLoader.SmartLoad(icon.src, width, height, microModule.blobFetchHook) }
  AppIcon(
    imageResult,
    modifier = modifier.requiredSize(width, height),
    iconMaskable = icon?.let { icon.purpose.contains(ImageResourcePurposes.Maskable) } ?: false,
    iconMonochrome = icon?.let { icon.purpose.contains(ImageResourcePurposes.Monochrome) } ?: false,
    containerAlpha = containerAlpha,
  )
}
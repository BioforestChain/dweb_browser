package org.dweb_browser.browser.desk

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.window.render.imageFetchHook
import kotlin.math.min


@Composable
fun NewDesktopView(
  desktopController: DesktopController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val apps = remember { mutableStateListOf<DesktopAppModel>() }

  var blur by remember { mutableStateOf(false) }
  val blurValue by animateIntAsState(
    targetValue = if (blur) 5 else 0
  )

  var popUpIndex by remember { mutableStateOf<Int?>(null) }

  val scope = rememberCoroutineScope()

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  var popUpApp by remember { mutableStateOf<DesktopAppModel?>(null) }

  val toRunningApps by remember { mutableStateOf(mutableSetOf<String>()) }

  var textWord by remember { mutableStateOf("") }

  fun doGetApps() {
    scope.launch {
      val installApps = desktopController.getDesktopApps().map {
        val icon = it.icons.firstOrNull()?.src ?: ""
        val isSystemApp = desktopController.isSystermApp(it.mmid)
        val runStatus = if (it.running) {
          toRunningApps.remove(it.mmid)
          DesktopAppModel.DesktopAppRunStatus.RUNNING
        } else if (toRunningApps.contains(it.mmid)) {
          DesktopAppModel.DesktopAppRunStatus.TORUNNING
        } else {
          DesktopAppModel.DesktopAppRunStatus.NONE
        }

        val oldApp = apps.find { oldApp ->
          oldApp.mmid == it.mmid
        }

        oldApp?.copy(running = runStatus)
          ?: DesktopAppModel(
            it.name,
            it.mmid,
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
    }
      .collectIn(scope) {
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
    if (index != -1) {
      val oldApp = apps[index]
      if (oldApp.running == DesktopAppModel.DesktopAppRunStatus.NONE) {
        apps[index] = oldApp.copy(running = DesktopAppModel.DesktopAppRunStatus.TORUNNING)
        toRunningApps.add(mmid)
      }
    } else {
      return
    }

    scope.launch {
      desktopController.open(mmid)
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

  fun doShare(mmid: String) {
    scope.launch {
      desktopController.share(mmid)
    }
  }

  fun doShowPopUp(index: Int) {
    if (index >= apps.count()) return
    val app = apps[index]
    app.image?.let {
      blur = true
      popUpIndex = index
      popUpApp = app
    }
  }

  fun doHidePopUp() {
    popUpApp = null
    popUpIndex = null
    blur = false
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // content
    Box(
      modifier = Modifier.fillMaxSize().blur(blurValue.dp),
      contentAlignment = Alignment.TopStart
    ) {
      desktopWallpaperView(
        desktopBgCircleCount(),
        modifier = Modifier
          .clickableWithNoEffect {
            doHideKeyboard()
          }
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)
          .padding(top = desktopTap())
          .clickableWithNoEffect {
            doHideKeyboard()
          }
      ) {

        desktopSearchBar(
          textWord,
          Modifier.windowInsetsPadding(WindowInsets.safeGestures).blur(blurValue.dp),
          ::doSearch,
          ::doHideKeyboard
        )

        LazyVerticalGrid(
          columns = desktopGridLayout(),
          modifier = Modifier.fillMaxSize()
        ) {

          itemsIndexed(apps) { index, app ->
            var alpha = 1F
            if (popUpIndex != null) {
              alpha = if (index == popUpIndex) 0F else 1F
            }

            AppItem(
              modifier = Modifier.alpha(alpha).pointerInput(Unit) {
                detectTapGestures(onTap = {
                  doOpen(app.mmid)
                }, onLongPress = {
                  doShowPopUp(index)
                })
              },
              app = app,
              hook = microModule.imageFetchHook,
            )
          }
        }
      }
    }


    // floating
    if (popUpApp?.image != null) {
      BoxWithConstraints(contentAlignment = Alignment.TopStart,
        modifier = Modifier
          .fillMaxSize()
          .clickableWithNoEffect {
            doHidePopUp()
          }
      ) {

        var popSize by remember { mutableStateOf(IntSize.Zero) }
        var popAlpha by remember { mutableStateOf(0f) }
        val density = LocalDensity.current.density
        val boxWidth = constraints.maxWidth
        val boxHeight = constraints.maxHeight

        val scale = 1.05
        val offX = popUpApp!!.offSet!!.x / density
        val offY = popUpApp!!.offSet!!.y / density
        val width = (popUpApp!!.size!!.width / density) * scale
        val height = (popUpApp!!.size!!.height / density) * scale

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(width = width.dp, height = height.dp).offset(offX.dp, offY.dp)
            .aspectRatio(1.0f)
            .background(color = Color.White, shape = RoundedCornerShape(16.dp))
            .clickableWithNoEffect {
              doOpen(popUpApp!!.mmid)
              doHidePopUp()
            }
        ) {
          Image(popUpApp!!.image!!, contentDescription = null)
        }

        val popOffX = min((offX * density), (boxWidth - popSize.width).toFloat()).toInt()
        var popOffY = ((offY + height + 15) * density).toInt()
        if (popOffY + popSize.height + 15 > boxHeight) {
          popOffY = ((offY - popSize.height - 15) * density).toInt()
        }


        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                x = popOffX,
                y = popOffY
              )
            }.alpha(popAlpha)
        ) {
          moreAppDisplay(
            popUpApp!!,
            ::doQuit,
            ::doDetail,
            ::doUninstall,
            ::doShare,
            ::doHidePopUp
          ) {
            popSize = it
            popAlpha = 1f
          }
        }
      }
    }
  }
}

private typealias AppItemAction = (String) -> Unit

@Composable
fun AppItem(
  modifier: Modifier,
  app: DesktopAppModel,
  hook: FetchHook,
) {

  Box(
    modifier = (modifier.fillMaxSize())
      .padding(5.dp),
    contentAlignment = Alignment.TopStart,
  ) {
    Column(
      modifier = Modifier
        .align(Alignment.CenterStart)
    ) {

      DeskAppIcon(
        app,
        hook
      )
      Text(
        text = app.name,
        fontSize = 10.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
          .background(color = Color.White, shape = RoundedCornerShape(4.dp))
      )
    }
  }
}

@Composable
fun DeskAppIcon(
  app: DesktopAppModel,
  hook: FetchHook,
  width: Dp = 50.dp,
  height: Dp = 50.dp
) {
  val toRuningAnimation = rememberInfiniteTransition()
  val offY by toRuningAnimation.animateFloat(
    initialValue = 0F,
    targetValue = 8F,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = EaseInOut),
      repeatMode = RepeatMode.Reverse
    )
  )
  Box(contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
      .offset(
        0.dp,
        if (app.running == DesktopAppModel.DesktopAppRunStatus.TORUNNING) offY.dp else 0.dp
      )
      .aspectRatio(1.0f)
      .background(color = Color.White, shape = RoundedCornerShape(16.dp))
      .onGloballyPositioned {
        app.size = it.size
        app.offSet = it.positionInWindow().toIntOffset(1F)
      }

  ) {
    val imageResult = PureImageLoader.SmartLoad(app.icon, width, height, hook)
    if (imageResult.isSuccess) {
      app.image = imageResult.success
      Image(imageResult.success!!, contentDescription = null)
    } else {
      Image(Icons.TwoTone.Image, contentDescription = null)
    }
  }
}

private data class MoreAppModel(
  private val mmid: String,
  val type: MoreAppModelType,
  private val action: AppItemAction,
  val enable: Boolean
) {
  fun doAction() {
    action(mmid)
  }
}

private enum class MoreAppModelType {
  OFF {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData("退出", Icons.Outlined.HighlightOff)
  },

  DETAIL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData("详情", Icons.Outlined.Description)
  },

  UNINSTALL {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData("删除", Icons.Outlined.Delete)
  },

  SHARE {
    override val data: MoreAppModelTypeData
      get() = MoreAppModelTypeData("分享", Icons.Outlined.Share)
  };

  data class MoreAppModelTypeData(val title: String, val icon: ImageVector)

  abstract val data: MoreAppModelTypeData
}

@Composable
private fun moreAppItemsDisplay(displays: List<MoreAppModel>, dismiss: () -> Unit) {
  Row(
    modifier = Modifier.padding(8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    displays.forEach {
      Column(
        modifier = Modifier.width(60.dp).clickable(enabled = it.enable) {
          it.doAction()
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
          fontSize = 12.sp,
          color = if (it.enable) Color.Black else Color.Gray
        )
      }
    }
  }
}

expect fun desktopGridLayout(): GridCells
expect fun desktopTap(): Dp
expect fun desktopBgCircleCount(): Int

@Composable
fun moreAppDisplay(
  app: DesktopAppModel,
  quit: AppItemAction,
  detail: AppItemAction,
  uninstall: AppItemAction,
  share: AppItemAction,
  dismiss: () -> Unit,
  onSize: (IntSize) -> Unit,
) {

  val displays = mutableListOf<MoreAppModel>()
  displays.add(
    MoreAppModel(
      app.mmid,
      MoreAppModelType.OFF,
      quit,
      app.running == DesktopAppModel.DesktopAppRunStatus.RUNNING
    )
  )
  if (!app.isSystermApp) {
    displays.add(MoreAppModel(app.mmid, MoreAppModelType.DETAIL, detail, true))
    displays.add(MoreAppModel(app.mmid, MoreAppModelType.UNINSTALL, uninstall, true))
  }
  displays.add(MoreAppModel(app.mmid, MoreAppModelType.SHARE, share, false))

  Box(
    modifier = Modifier.clip(RoundedCornerShape(8.dp))
      .background(color = Color.White.copy(alpha = 0.6F))
      .onSizeChanged {
        onSize(it)
      }
  ) {

    moreAppItemsDisplay(displays, dismiss)
  }
}

@Composable
fun desktopSearchBar(
  textWord: String,
  modifier: Modifier,
  search: (String) -> Unit,
  hideKeyBoad: () -> Unit
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
      keyboardActions = KeyboardActions(
        onSearch = {
          hideKeyBoad()
          search(searchWord.value.text)
          searchWord.value = TextFieldValue("")
        }),
      decorationBox = {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxWidth()
        ) {

          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp).offset {
              searchIconOffset.value
            }
          )

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
      modifier = Modifier
        .onFocusChanged {
          doSearchTirggleAnimation(it.isFocused, constraints.maxWidth)
        }
        .fillMaxWidth(searchBarWidthRadio.value)
        .height(44.dp)
        .background(color = Color.Transparent)
        .clip(RoundedCornerShape(8.dp))
        .border(1.dp, color = Color.White, shape = RoundedCornerShape(22.dp))
        .windowInsetsPadding(WindowInsets.safeGestures)
    )
  }
}

data class DesktopAppModel(
  val name: String,
  val mmid: MMID,
  val icon: String,
  val isSystermApp: Boolean,
  var running: DesktopAppRunStatus = DesktopAppRunStatus.NONE,
  var image: ImageBitmap? = null,
  var size: IntSize? = null,
  var offSet: IntOffset? = null,
  val id: UUID = randomUUID()
) {

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

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
  return pointerInput(Unit) {
    detectTapGestures(onTap = { onClick() })
  }
}

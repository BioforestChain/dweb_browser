package org.dweb_browser.browser.desk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.window.render.imageFetchHook


@Composable
fun NewDesktopView(
  taskbarController: TaskbarController,
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

  fun doGetApps() {
    scope.launch {
      println("Mike desktop update")
      val installApps = taskbarController.desktopController.getDesktopApps().map {
        val icon = it.icons.firstOrNull()?.src ?: ""
        val isSystermApp = taskbarController.desktopController.isSystermApp(it.mmid)
        println("Mike app: ${it.mmid} ${it.running}")
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
            isSystermApp,
            runStatus,
          )
      }
      apps.clear()
      apps.addAll(installApps)
    }
  }

  DisposableEffect(Unit) {
    val off = taskbarController.desktopController.onUpdate {
      doGetApps()
    }
    onDispose {
      off()
    }
  }

  fun doHideKeyboard() {
    keyboardController?.hide()
    focusManager.clearFocus()
  }

  fun doSearch(words: String) {
    scope.launch {
      taskbarController.desktopController.search(words)
    }
  }

  fun doOpen(mmid: String) {
    toRunningApps.add(mmid)
    scope.launch {
      taskbarController.desktopController.open(mmid)
    }
  }

  fun doQuit(mmid: String) {
    toRunningApps.remove(mmid)
    scope.launch {
      taskbarController.desktopController.quit(mmid)
    }
  }

  fun doDetail(mmid: String) {
    scope.launch {
      taskbarController.desktopController.detail(mmid)
    }
  }

  fun doUninstall(mmid: String) {
    scope.launch {
      taskbarController.desktopController.uninstall(mmid)
    }
  }

  fun doShare(mmid: String) {
    scope.launch {
      taskbarController.desktopController.share(mmid)
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

  doGetApps()

  Box(modifier = Modifier.fillMaxSize()) {
    // content
    Box(
      modifier = Modifier.fillMaxSize().blur(blurValue.dp),
      contentAlignment = Alignment.TopStart
    ) {
      desktopBackgroundView(
        modifier = Modifier.noRippleClickable {
          doHideKeyboard()
        }
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)
          .noRippleClickable {
            doHideKeyboard()
          }
      ) {

        desktopSearchBar(modifier = Modifier.blur(blurValue.dp), ::doSearch, ::doHideKeyboard)

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
      val density = LocalDensity.current.density
      val scale = 1.05
      val offX = popUpApp!!.offSet!!.x / density
      val offY = popUpApp!!.offSet!!.y / density
      val width = (popUpApp!!.size!!.width / density) * scale
      val height = (popUpApp!!.size!!.height / density) * scale
      Box(contentAlignment = Alignment.TopStart,
        modifier = Modifier.fillMaxSize()
          .clickable {}
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(width = width.dp, height = height.dp).offset(offX.dp, offY.dp)
            .aspectRatio(1.0f)
            .background(color = Color.White, shape = RoundedCornerShape(16.dp))
        ) {
          Image(popUpApp!!.image!!, contentDescription = null)
        }

        Popup(
          onDismissRequest = ::doHidePopUp,
          offset = IntOffset(
            x = (offX * density).toInt(),
            y = ((offY + height + 15) * density).toInt()
          )
        ) {
          moreAppDisplay(popUpApp!!, ::doQuit, ::doDetail, ::doUninstall, ::doShare, ::doHidePopUp)
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
      animation = tween(1000, easing = LinearEasing),
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

@Composable
fun moreAppDisplay(
  app: DesktopAppModel,
  quit: AppItemAction,
  detail: AppItemAction,
  uninstall: AppItemAction,
  share: AppItemAction,
  dismiss: () -> Unit
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
  ) {

    moreAppItemsDisplay(displays, dismiss)
  }
}

@Composable
fun desktopSearchBar(modifier: Modifier, search: (String) -> Unit, hideKeyBoad: () -> Unit) {

  var searchWord = remember {
    mutableStateOf(TextFieldValue(""))
  }

  var moved by remember { mutableStateOf(0) }
  val offset by animateIntOffsetAsState(
    targetValue = if (moved < 0) {
      IntOffset(moved, 0)
    } else {
      IntOffset.Zero
    },
    label = "offset"
  )

  var searchWidthRatio by remember { mutableStateOf(0.5) }
  val searchWidthFraction by animateFloatAsState(
    targetValue = searchWidthRatio.toFloat(),
    label = "Search width fraction"
  )

  fun doSearchTirggleAnimation(on: Boolean, searchTextFieldWidth: Int) {
    searchWidthRatio = if (on) 1.0 else 0.5
    moved = -1 * if (on) (searchTextFieldWidth / 2 - 50) else 0
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
            modifier = Modifier.offset {
              offset
            }
              .padding(8.dp)
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
      modifier = Modifier
        .onFocusChanged {
          doSearchTirggleAnimation(it.isFocused, constraints.maxWidth)
        }
        .fillMaxWidth(searchWidthFraction)
        .height(44.dp)
        .background(color = Color.Transparent)
        .clip(RoundedCornerShape(22.dp))
        .border(1.dp, color = Color.Gray, shape = RoundedCornerShape(22.dp))
        .shadow(
          elevation = 3.dp, shape = RoundedCornerShape(22.dp)
        )
        .windowInsetsPadding(WindowInsets.safeGestures)
    )
  }
}

@Composable
fun desktopBackgroundView(modifier: Modifier) {
  Box(modifier = modifier.fillMaxSize().background(color = Color.Transparent)) {
  //TODO: Mike 这边需要设置背景图片。
  //    AsyncImage(
  //      "https://images.pexels.com/photos/22866338/pexels-photo-22866338.jpeg",
  //      contentDescription = null,
  //      contentScale = ContentScale.FillBounds
  //    )
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

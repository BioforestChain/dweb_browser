package org.dweb_browser.browser.desk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.hex
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.window.render.imageFetchHook
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random


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

  fun doGetApps() {
    scope.launch {
      val installApps = desktopController.getDesktopApps().map {
        val icon = it.icons.firstOrNull()?.src ?: ""
        val isSystermApp = desktopController.isSystermApp(it.mmid)
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
  }

  fun doSearch(words: String) {
    scope.launch {
      desktopController.search(words)
    }
  }

  fun doOpen(mmid: String) {
    toRunningApps.add(mmid)
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

  doGetApps()

  Box(modifier = Modifier.fillMaxSize()) {
    // content
    Box(
      modifier = Modifier.fillMaxSize().blur(blurValue.dp),
      contentAlignment = Alignment.TopStart
    ) {
      desktopBackgroundView(
        modifier = Modifier
          .noRippleClickable {
            doHideKeyboard()
          }
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent)
          .padding(top = desktopTap())
          .noRippleClickable {
            doHideKeyboard()
          }
      ) {

        desktopSearchBar(
          modifier = Modifier.windowInsetsPadding(WindowInsets.safeGestures).blur(blurValue.dp),
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
          .noRippleClickable {
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
            .noRippleClickable {
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
fun desktopSearchBar(modifier: Modifier, search: (String) -> Unit, hideKeyBoad: () -> Unit) {

  val searchWord = remember {
    mutableStateOf(TextFieldValue(""))
  }

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

@Composable
fun desktopBackgroundView(modifier: Modifier) {

  val circles = remember {
    val result = mutableStateListOf<DesktopBgCircleModel>()
    result.addAll(DesktopBgCircleModel.randomCircle())
    result
  }

  var hour by remember {
    val currentMoment: Instant = Clock.System.now()
    val datetimeInSystemZone: LocalDateTime =
      currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    mutableStateOf<Int>(datetimeInSystemZone.hour)
  }

  LaunchedEffect(Unit) {
    suspend fun observerHourChange(action: (Int) -> Unit) {
      val currentMoment: Instant = Clock.System.now()
      val datetimeInSystemZone: LocalDateTime =
        currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
      val triggleSeconds = (60 - datetimeInSystemZone.minute) * 60 - datetimeInSystemZone.second
      delay(triggleSeconds.toLong() * 1000)
      action(datetimeInSystemZone.hour)
    }

    while (true) {
      observerHourChange { toHour ->
        val newCircles = circles.map {
          it.copy(color = DesktopBgCircleModel.randomColor(toHour))
        }
        circles.clear()
        circles.addAll(newCircles)
        hour = toHour
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
  ) {

    RotatingLinearGradientBox(hour, modifier = Modifier.zIndex(-1f))

    circles.forEach {
      val randomX = random(constraints.maxWidth * 0.2f)
      val randomY = random(constraints.maxWidth * 0.2f)
      DesktopBgCircle(it, randomX, randomY)
    }
  }
}

@Composable
fun RotatingLinearGradientBox(hour: Int, modifier: Modifier) {
  // 当前时间的角度 + 90f， 默认是从270度开始的，所以添加90度的偏移
  val angle = hour.toFloat() / 24f * 360f + 90f
  // 将角度转换为弧度, 逆时针旋转
  val angleRad = angle * PI / 180
  // 计算起点和终点
  val start = Offset(
    x = 0.5f + 0.5f * cos(angleRad).toFloat(),
    y = 0.5f - 0.5f * sin(angleRad).toFloat()
  )
  val end = Offset(
    x = 0.5f - 0.5f * cos(angleRad).toFloat(),
    y = 0.5f + 0.5f * sin(angleRad).toFloat()
  )

  val bgColors = desktopBgPrimaryColors(hour).map {
    Color.hex(it)!!
  }

  Canvas(
    modifier = modifier.fillMaxSize()
  ) {
    drawRect(
      brush = Brush.linearGradient(
        colors = bgColors,
        start = Offset(size.width * start.x, size.height * start.y),
        end = Offset(size.width * end.x, size.height * end.y)
      )
    )
  }
}


@Composable
fun BoxWithConstraintsScope.DesktopBgCircle(
  model: DesktopBgCircleModel,
  animationX: Float,
  animationY: Float
) {
  val scope = rememberCoroutineScope()
  val scaleXValue = remember { Animatable(1f) }
  val scaleYValue = remember { Animatable(1f) }
  val transformXValue = remember { Animatable(1f) }
  val transformYValue = remember { Animatable(1f) }
  val colorValue = remember { androidx.compose.animation.Animatable(Color.Transparent) }


  fun doBubbleAnimation() {
    scope.launch {
      val scaleAnimationSpec = tween<Float>(
        durationMillis = 500,
        delayMillis = 0,
        easing = FastOutSlowInEasing
      )

      val transformAnimationSpec = tween<Float>(
        durationMillis = 2000,
        delayMillis = 0,
        easing = FastOutSlowInEasing
      )

      launch {
        scaleXValue.animateTo(1.05f, scaleAnimationSpec)
        scaleXValue.animateTo(0.95f, scaleAnimationSpec)
        scaleXValue.animateTo(0.97f, scaleAnimationSpec)
        scaleXValue.animateTo(1.0f, scaleAnimationSpec)
      }

      launch {
        scaleYValue.animateTo(0.95f, scaleAnimationSpec)
        scaleYValue.animateTo(1.05f, scaleAnimationSpec)
        scaleYValue.animateTo(0.97f, scaleAnimationSpec)
        scaleYValue.animateTo(1.00f, scaleAnimationSpec)
      }

      launch {
        transformXValue.animateTo(animationX, transformAnimationSpec)
      }

      launch {
        transformYValue.animateTo(animationY, transformAnimationSpec)
      }

      launch {
        colorValue.animateTo(model.color, tween(3000, 0, LinearEasing))
      }
    }
  }

  LaunchedEffect(model.offset, animationX, animationY) {
    doBubbleAnimation()
  }
  val width = constraints.maxWidth
  val height = constraints.maxHeight
  Box(modifier = Modifier
    .offset {
      Offset(
        x = model.offset.x * width / 2,
        y = model.offset.y * height / 2
      )
        .toIntOffset(1F)
    }
    .graphicsLayer {
      scaleX = scaleXValue.value
      scaleY = scaleYValue.value
      translationX = transformXValue.value
      translationY = transformYValue.value
    }
    .blur(model.blur.dp)
  )
  {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = colorValue.value,
        model.radius * width / 4f,
      )
    }
  }

}


private fun random(times: Float = 1f) = (Random.nextFloat() - 0.5f) * times

/*
        // 晚上
        19, 20, 21, 22: multiply #00C5DF #00C5DF #00C5D #F2371F
        // 极光
        23, 0, 1: overlay #315787 #B8B5D6 #64ADBD #6B93C6 #000000
        // 凌晨
        1, 2, 3, 4: multiply #18A0FB #1BC47D
        // 日出
        5, 6: luminosity #18A0FB #1BC47D #18A0FB #FFC700 #FFC700 #F2371F
        // 早上
        7,8,9,: overlay #18A0FB #907CFF
        // 中午
        10,11,12,13: overlay #EE46D3 #907CFF
        // 下午
        14, 15, 16: overlay #FFC700 #EE46D3
        // 日落
        17,18: overlay #F2371F #18A0FB #907CFF #FFC700
* */

private typealias ColorString = String

private fun desktopBgPrimaryColors(hour: Int? = null): List<ColorString> {

  val toHour = if (hour != null) hour else {
    val clock = Clock.System.now()
    val timeZone = clock.toLocalDateTime(TimeZone.currentSystemDefault())
    timeZone.hour
  }

  return when (toHour) {
    1, 2, 3, 4 -> listOf("#18A0FB", "#1BC47D")
    5, 6 -> listOf("#3a1c71", "#d76d77", "#ffaf7b")
    7, 8, 9 -> listOf("#18A0FB", "#907CFF")
    10, 11, 12, 13 -> listOf("#EE46D3", "#907CFF")
    14, 15, 16 -> listOf("#FFC700", "#EE46D3")
    17, 18, 19, 20, 21, 22 -> listOf("#18A0FB", "#7fffd4")
    23, 0 -> listOf("#315787", "#B8B5D6", "#64ADBD", "#000000")
    else -> listOf("#18A0FB", "#1BC47D")
  }
}


data class DesktopBgCircleModel(
  var offset: Offset,
  var radius: Float,
  var color: Color,
  var blur: Int,
) {
  companion object {
    fun randomCircle(): List<DesktopBgCircleModel> {
      val list = mutableListOf<DesktopBgCircleModel>()
      val count = desktopBgCircleCount()

      val offset = {
        Offset(
          Random.nextFloat() * 2f - 1f,
          Random.nextFloat() * 2f - 1f,
        )
      }

      val radius = {
        Random.nextFloat()
      }

      val color = {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInSystemZone: LocalDateTime =
          currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
        randomColor(datetimeInSystemZone.hour)
      }

      val blur = {
        Random.nextInt(5) + 1
      }

      var i = 0
      while (i < count) {
        i += 1;
        list.add(DesktopBgCircleModel(offset(), radius(), color(), blur()))
      }

      list.sortBy {
        it.blur
      }

      return list.reversed()
    }

    fun randomColor(hour: Int): Color {
      val colors = desktopBgPrimaryColors(hour)
      val colorStart = colors.first()
      val colorEnd = colors.last()

      fun getColor(range: IntRange): Int {
        val c0 = colorStart.substring(range).toInt(16)
        val c1 = colorEnd.substring(range).toInt(16)
        return if (c0 == c1) {
          255
        } else if (c0 < c1) {
          (c0..c1).random()
        } else {
          (c1..c0).random()
        }
      }

      val color = Color(getColor(1..2), getColor(3..4), getColor(5..6))
      return color
    }
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

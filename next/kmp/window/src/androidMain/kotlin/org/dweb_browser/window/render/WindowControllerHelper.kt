package org.dweb_browser.window.render

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.android.WindowInsetsHelper
import org.dweb_browser.helper.compose.noLocalProvidedFor
import org.dweb_browser.helper.compose.theme.md_theme_dark_inverseOnSurface
import org.dweb_browser.helper.compose.theme.md_theme_dark_onSurface
import org.dweb_browser.helper.compose.theme.md_theme_dark_surface
import org.dweb_browser.helper.compose.theme.md_theme_light_inverseOnSurface
import org.dweb_browser.helper.compose.theme.md_theme_light_onSurface
import org.dweb_browser.helper.compose.theme.md_theme_light_surface
import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchRequest
import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchResponse
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.window.core.WindowBounds
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.constant.WindowColorScheme
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.constant.WindowPropertyKeys
import org.dweb_browser.window.core.helper.asWindowStateColorOr
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


/**
 * 提供一个计算函数，来获得一个在Compose中使用的 state
 */
@Composable
fun <T> WindowController.watchedState(
  key: Any? = null,
  policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
  filter: ((change: Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
  watchKey: WindowPropertyKeys? = null,
  watchKeys: Set<WindowPropertyKeys>? = null,
  getter: WindowState .() -> T,
): State<T> = remember(key) {
  val rememberState = mutableStateOf(getter.invoke(state), policy);
  val off = state.observable.onChange {
    if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
        it.key
      ) else true) && filter?.invoke(it) != false
    ) {
      rememberState.value = getter.invoke(state)
    }
  }
  Pair(rememberState, off)
}.let { (rememberState, off) ->
  DisposableEffect(off) {
    onDispose {
      off()
    }
  }
  rememberState
}

/**
 * 触发 window 聚焦状态的更新事件监听
 */
fun WindowController.emitFocusOrBlur(focused: Boolean) {
  coroutineScope.launch {
    if (focused) {
      focus()
    } else {
      blur()
    }
  }
}

val inMoveStore = WeakHashMap<WindowController, MutableState<Boolean>>()

/**
 * 窗口是否在移动中
 */
val WindowController.inMove
  get() = inMoveStore.getOrPut(this) { mutableStateOf(false) }

/**
 * 移动窗口的控制器
 */
fun Modifier.windowMoveAble(win: WindowController) = this.pointerInput(win) {
  /// 触摸窗口的时候，聚焦，并且提示可以移动
  detectTapGestures(
    // touchStart 的时候，聚焦移动
    onPress = {
      win.inMove.value = true
      win.emitFocusOrBlur(true)
    },
    /// touchEnd 的时候，取消移动
    onTap = {
      win.inMove.value = false
    },
    onLongPress = {
      win.inMove.value = false
    },
  )
}.pointerInput(win) {
  /// 拖动窗口
  detectDragGestures(
    onDragStart = {
      win.inMove.value = true
      /// 开始移动的时候，同时进行聚焦
      win.emitFocusOrBlur(true)
    },
    onDragEnd = {
      win.inMove.value = false
    },
    onDragCancel = {
      win.inMove.value = false
    },
  ) { change, dragAmount ->
    change.consume()
    win.state.updateMutableBounds {
      left += dragAmount.x / density
      top += dragAmount.y / density
    }
  }
}

val inResizeStore = WeakHashMap<WindowController, MutableState<Boolean>>()

/** 窗口是否在调整大小中 */
val WindowController.inResize get() = inResizeStore.getOrPut(this) { mutableStateOf(false) }

/** 基于窗口左下角进行调整大小 */
fun Modifier.windowResizeByLeftBottom(win: WindowController) = this.pointerInput(win) {
  var inResize by win.inResize
  detectDragGestures(
    onDragStart = { inResize = true },
    onDragEnd = { inResize = false },
    onDragCancel = { inResize = false },
  ) { change, dragAmount ->
    change.consume()
    win.state.updateBounds {
      copy(
        left = left + dragAmount.x / density,
        width = width - dragAmount.x / density,
        height = height + dragAmount.y / density,
      )
    }
  }
}

/** 基于窗口右下角进行调整大小 */
fun Modifier.windowResizeByRightBottom(win: WindowController) = this.pointerInput(win) {
  var inResize by win.inResize
  detectDragGestures(
    onDragStart = { inResize = true },
    onDragEnd = { inResize = false },
    onDragCancel = { inResize = false },
  ) { change, dragAmount ->
    change.consume()
    win.state.updateBounds {
      copy(
        width = width + dragAmount.x / density,
        height = height + dragAmount.y / density,
      )
    }
  }
}


val LocalWindowLimits = compositionLocalOf<WindowLimits> { noLocalProvidedFor("WindowLimits") }
val LocalWindowController =
  compositionLocalOf<WindowController> { noLocalProvidedFor("WindowController") }
val LocalWindowsManager =
  compositionLocalOf<WindowsManager<*>> { noLocalProvidedFor("WindowsManager") }

data class WindowLimits(
  val minWidth: Float,
  val minHeight: Float,
  val maxWidth: Float,
  val maxHeight: Float,
  /**
   * 窗口的最小缩放
   *
   * 和宽高不一样，缩放意味着保持宽高不变的情况下，将网页内容缩小，从而可以展示更多的网页内容
   */
  val minScale: Double,
  /**
   * 窗口顶部的基本高度
   */
  val topBarBaseHeight: Float,
  /**
   * 窗口底部的基本高度
   */
  val bottomBarBaseHeight: Float,
)

@Composable
fun WindowController.watchedIsMaximized() =
  watchedState(watchKey = WindowPropertyKeys.Mode) { mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN }

@Composable
fun WindowController.watchedBounds() = watchedState(watchKey = WindowPropertyKeys.Bounds) { bounds }

/**
 * 根据约束配置，计算出最终的窗口大小与坐标
 */
@Composable
fun WindowController.calcWindowBoundsByLimits(
  limits: WindowLimits
): WindowBounds {
  val maximize by watchedIsMaximized()
  return if (maximize) {
    inMove.value = false
    state.updateBounds {
      copy(
        left = 0f,
        top = 0f,
        width = limits.maxWidth,
        height = limits.maxHeight,
      )
    }
  } else {
    val layoutDirection = LocalLayoutDirection.current
    val bounds by watchedBounds()

    /**
     * 获取可触摸的空间
     */
    val safeGesturesPadding = WindowInsets.safeGestures.asPaddingValues();
    val winWidth = max(bounds.width, limits.minWidth)
    val winHeight = max(bounds.height, limits.minHeight)
    val safeLeftPadding = safeGesturesPadding.calculateLeftPadding(layoutDirection).value
    val safeTopPadding = safeGesturesPadding.calculateTopPadding().value
    val safeRightPadding = safeGesturesPadding.calculateRightPadding(layoutDirection).value
    val safeBottomPadding = safeGesturesPadding.calculateBottomPadding().value
    val minLeft = safeLeftPadding - winWidth / 2
    val maxLeft = limits.maxWidth - safeRightPadding - winWidth / 2
    val minTop = safeTopPadding
    val maxTop =
      limits.maxHeight - safeBottomPadding - limits.topBarBaseHeight // 确保 topBar 在可触摸的空间内
    state.updateBounds {
      copy(
        left = min(max(minLeft, bounds.left), maxLeft),
        top = min(max(minTop, bounds.top), maxTop),
        width = winWidth,
        height = winHeight,
      )
    }
  };
}

/**
 * 根据约束配置，计算出最终的窗口边距布局
 */
@Composable
fun WindowController.calcWindowPaddingByLimits(limits: WindowLimits): WindowPadding {
  val maximize by watchedIsMaximized()
  val bounds by watchedBounds()
  val bottomBarTheme by watchedState(watchKey = WindowPropertyKeys.BottomBarTheme) { bottomBarTheme }
  val keyboardOverlaysContent by watchedState { keyboardOverlaysContent }
  val keyboardInsetBottom by watchedState { keyboardInsetBottom }

  val topHeight: Float;
  val bottomHeight: Float
  val leftWidth: Float;
  val rightWidth: Float;
  val borderRounded: WindowPadding.CornerRadius;
  val contentRounded: WindowPadding.CornerRadius;
  val contentSize: WindowPadding.ContentSize

  /// 一些共有的计算
  val windowFrameSize = if (maximize) 3f else 5f

  /**
   * 不同的底部栏风格有不同的高度
   */
  val bottomThemeHeight = when (bottomBarTheme) {
    WindowBottomBarTheme.Immersion -> limits.bottomBarBaseHeight// 因为底部要放置一些信息按钮，所以我们会给到底部一个基本的高度
    WindowBottomBarTheme.Navigation -> max(limits.bottomBarBaseHeight, 32f) // 要有足够的高度放按钮和基本信息
  };

  fun max(val1: Float, val2: Float, val3: Float) = max(max(val1, val2), val3)

  if (maximize) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current.density
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeGesturesPadding = WindowInsets.safeGestures.asPaddingValues()
    topHeight = max(safeDrawingPadding.calculateTopPadding().value, windowFrameSize)

    /**
     * 底部是系统导航栏，这里我们使用触摸安全的区域来控制底部高度，这样可以避免底部抖动
     * 不该使用 safeDrawing，它会包含 ime 的高度
     */
    bottomHeight = max(
      // 这里默认使用 safeGestures ，因为它只包含底部导航栏的高度，是稳定的
      safeGesturesPadding.calculateBottomPadding().value,
      bottomThemeHeight,
      windowFrameSize,
    );
    /**
     * 即便是最大化模式下，我们仍然需要有一个强调边框。
     * 这个边框存在的意义有：
     * 1. 强调是窗口模式，而不是全屏模式
     * 2. 养成用户的视觉习惯，避免某些情况下有人使用视觉手段欺骗用户，窗口模式的存在将一切限定在一个规则内，可以避免常规视觉诈骗
     * 3. 全屏模式虽然会移除窗口，但是会有一些其它限制，比如但需要进行多窗口交互的时候，这些窗口边框仍然会显示出来
     */
    leftWidth = max(safeDrawingPadding.calculateLeftPadding(layoutDirection).value, windowFrameSize)
    rightWidth =
      max(safeDrawingPadding.calculateRightPadding(layoutDirection).value, windowFrameSize)
    borderRounded = WindowPadding.CornerRadius.from(0) // 全屏模式下，外部不需要圆角
    contentRounded = WindowPadding.CornerRadius.from(
      WindowInsetsHelper.getCornerRadiusTop(viewController.androidContext, density, 16f),
      WindowInsetsHelper.getCornerRadiusBottom(viewController.androidContext, density, 16f)
    )
    contentSize = WindowPadding.ContentSize(
      bounds.width - leftWidth - rightWidth,
      bounds.height - topHeight - keyboardInsetBottom - bottomHeight,
    )
  } else {

    borderRounded =
      WindowPadding.CornerRadius.from(16) // TODO 这里应该使用 WindowInsets#getRoundedCorner 来获得真实的无力圆角
    contentRounded = borderRounded / sqrt(2f)
    topHeight = max(limits.topBarBaseHeight, windowFrameSize);
    bottomHeight = max(bottomThemeHeight, windowFrameSize)
    leftWidth = windowFrameSize;
    rightWidth = windowFrameSize;
    contentSize = WindowPadding.ContentSize(
      bounds.width - leftWidth - rightWidth,
      bounds.height - topHeight - keyboardInsetBottom - bottomHeight,
    )
  }
  return WindowPadding(
    topHeight, bottomHeight, leftWidth, rightWidth, borderRounded, contentRounded, contentSize
  )
}

val LocalWindowPadding = compositionLocalOf<WindowPadding> { noLocalProvidedFor("WindowPadding") }


/**
 * 窗口边距布局配置
 */
data class WindowPadding(
  val top: Float, val bottom: Float, val left: Float, val right: Float,
  /**
   * 外部圆角
   */
  val boxRounded: CornerRadius,
  /**
   * 内容圆角
   */
  val contentRounded: CornerRadius, val contentBounds: ContentSize
) {
  val start get() = left
  val end get() = right

  data class CornerRadius(
    val topStart: Float, val topEnd: Float, val bottomStart: Float, val bottomEnd: Float
  ) {
    operator fun div(value: Float) =
      CornerRadius(topStart / value, topEnd / value, bottomStart / value, bottomEnd / value)

    fun toRoundedCornerShape() =
      RoundedCornerShape(topStart.dp, topEnd.dp, bottomStart.dp, bottomEnd.dp)

    companion object {
      fun from(radius: Float) = CornerRadius(radius, radius, radius, radius)
      fun from(radius: Int) = from(radius.toFloat())
      fun from(topRadius: Float, bottomRadius: Float) =
        CornerRadius(topRadius, topRadius, bottomRadius, bottomRadius)

      fun from(topRadius: Int, bottomRadius: Int) =
        from(topRadius.toFloat(), bottomRadius.toFloat())
    }
  }

  data class ContentSize(val width: Float, val height: Float)
};

/**
 * 计算窗口在对应布局时的内容缩放比例
 * 视图的宽高随着窗口的缩小而缩小，随着窗口的放大而放大，
 * 但这些缩放不是等比的，而是会以一定比例进行换算。
 *
 * 这个行为在桌面端也将会适用
 */
fun WindowController.calcContentScale(limits: WindowLimits, winEdge: WindowPadding): Float {
  /**
   * 计算进度
   */
  fun calcProgress(from: Float, now: Float, to: Float) = ((now - from) / (to - from)).toDouble()

  /**
   * 将动画进度还原成所需的缩放值
   */
  fun Double.toScale(minScale: Double, maxScale: Double = 1.0) =
    ((maxScale - minScale) * this) + minScale;

  val scaleProgress = max(
    calcProgress(limits.minWidth, winEdge.contentBounds.width, limits.maxWidth),
    calcProgress(limits.minHeight, winEdge.contentBounds.height, limits.maxHeight),
  )
  return scaleProgress.toScale(limits.minScale).let { if (it.isNaN()) 1f else it.toFloat() };
}

val LocalWindowControllerTheme =
  compositionLocalOf<WindowControllerTheme> { noLocalProvidedFor("WindowControllerTheme") }

data class WindowControllerTheme(
  val topContentColor: Color,
  val topBackgroundColor: Color,
  val themeColor: Color,
  val themeContentColor: Color,
  val onThemeColor: Color,
  val onThemeContentColor: Color,
  val bottomContentColor: Color,
  val bottomBackgroundColor: Color,
  val isDark: Boolean,
) {
  val winFrameBrush by lazy {
    Brush.verticalGradient(listOf(topBackgroundColor, themeColor, bottomBackgroundColor))
  }
  val themeContentDisableColor by lazy { themeContentColor.copy(alpha = themeContentColor.alpha * 0.2f) }
  val onThemeContentDisableColor by lazy { onThemeContentColor.copy(alpha = onThemeContentColor.alpha * 0.5f) }
  val topContentDisableColor by lazy { topContentColor.copy(alpha = topContentColor.alpha * 0.2f) }
  val bottomContentDisableColor by lazy { bottomContentColor.copy(alpha = bottomContentColor.alpha * 0.2f) }
}

/**
 * 构建颜色
 */
@Composable
fun WindowController.buildTheme(): WindowControllerTheme {
//  val calcThemeContentColor = watchedState(dark, watchKey = WindowPropertyKeys.ThemeColor) {
//    themeColor.asWindowStateColor(
//      md_theme_light_surface, md_theme_dark_surface, dark
//    )
//  }
  val colorScheme by watchedState { colorScheme }
  val isSystemInDark = isSystemInDarkTheme()
  val isDark = remember(colorScheme, isSystemInDark) {
    when (colorScheme) {
      WindowColorScheme.Normal -> isSystemInDark
      WindowColorScheme.Light -> false
      WindowColorScheme.Dark -> true
    }
  }
  val lightContent = remember(isDark) {
    if (isDark) md_theme_dark_onSurface else md_theme_light_inverseOnSurface
  }
  val darkContent = remember(isDark) {
    if (isDark) md_theme_dark_inverseOnSurface else md_theme_light_onSurface
  }

  fun calcContentColor(backgroundColor: Color) =
    if (backgroundColor.luminance() > 0.5f) darkContent else lightContent

  fun Color.convertToDark() = convert(ColorSpaces.Oklab).let { oklab ->
    if (oklab.red > 0.4f) {
      oklab.copy(red = (oklab.red * oklab.red).let { light -> if (light < 0.4f) light else 0.4f })
        .convert(ColorSpaces.Srgb)
    } else this
  }

  fun Color.convertToLight() = convert(ColorSpaces.Oklab).let { oklab ->
    if (oklab.red <= 0.6f) {
      oklab.copy(red = sqrt(oklab.red).let { light -> if (light >= 0.6f) light else 0.6f })
        .convert(ColorSpaces.Srgb)
    } else this
  }

  val themeColors by watchedState(
    isDark, watchKey = WindowPropertyKeys.ThemeColor
  ) {
    fun getThemeColor() = themeColor.asWindowStateColorOr(
      md_theme_light_surface, md_theme_dark_surface, isDark
    )


    val smartThemeColor = if (isDark) themeDarkColor.asWindowStateColorOr {
      getThemeColor().convertToDark()
    } else getThemeColor()
    val themeContentColor = calcContentColor(smartThemeColor)

    val themeOklabColor = smartThemeColor.convert(ColorSpaces.Oklab)

    val onThemeColor =
      themeOklabColor.copy(red = sqrt(themeOklabColor.red), alpha = 0.5f).convert(ColorSpaces.Srgb)
        .compositeOver(themeContentColor)
    val onThemeContentColor = themeOklabColor.copy(red = themeOklabColor.red * themeOklabColor.red)
      .convert(ColorSpaces.Srgb)
    Pair(Pair(smartThemeColor, themeContentColor), Pair(onThemeColor, onThemeContentColor))
  }
  val (themeColor, themeContentColor) = themeColors.first;
  val (onThemeColor, onThemeContentColor) = themeColors.second;

  val topBackgroundColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.TopBarBackgroundColor
  ) {
    fun getTopBarBackgroundColor() = topBarBackgroundColor.asWindowStateColorOr(themeColor)
    if (isDark) {
      topBarBackgroundDarkColor.asWindowStateColorOr { getTopBarBackgroundColor().convertToDark() }
    } else getTopBarBackgroundColor()
  }
  val topContentColor by watchedState(isDark, watchKey = WindowPropertyKeys.TopBarContentColor) {
    fun getTopBarContentColor() = topBarContentColor.asWindowStateColorOr {
      calcContentColor(
        topBackgroundColor
      )
    }
    if (isDark) {
      topBarContentDarkColor.asWindowStateColorOr { getTopBarContentColor().convertToLight() }
    } else getTopBarContentColor()
  }

  val bottomBackgroundColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.BottomBarBackgroundColor
  ) {
    fun getBottomBarBackgroundColor() = bottomBarBackgroundColor.asWindowStateColorOr(
      themeColor
    )
    if (isDark) {
      bottomBarBackgroundDarkColor.asWindowStateColorOr { getBottomBarBackgroundColor().convertToDark() }
    } else getBottomBarBackgroundColor()
  }
  val bottomContentColor by watchedState(
    isDark, watchKey = WindowPropertyKeys.BottomBarContentColor
  ) {
    fun getBottomBarContentColor() = bottomBarContentColor.asWindowStateColorOr {
      calcContentColor(
        bottomBackgroundColor
      )
    }
    if (isDark) {
      bottomBarContentDarkColor.asWindowStateColorOr { getBottomBarContentColor().convertToLight() }
    } else getBottomBarContentColor()
  }
  return WindowControllerTheme(
    themeColor = themeColor,
    themeContentColor = themeContentColor,
    onThemeColor = onThemeColor,
    onThemeContentColor = onThemeContentColor,
    topBackgroundColor = topBackgroundColor,
    topContentColor = topContentColor,
    bottomBackgroundColor = bottomBackgroundColor,
    bottomContentColor = bottomContentColor,
    isDark = isDark,
  )
}

val WindowSessionStore = WeakHashMap<WindowController, MutableMap<Any, Any>>()
val WindowController.sessionCache get() = WindowSessionStore.getOrPut(this) { ConcurrentHashMap() }

/**
 * 加载图标，如果有 state.microModule，那就让它去下载，否则就直接提供 string-url
 */
suspend fun WindowController.loadIconModel(iconUrl: String?) = when (val url = iconUrl) {
  null -> ImageState.Success(null)
  else -> when (val mm = state.constants.microModule) {
    null -> ImageState.Success(iconUrl)
    else -> sessionCache.getOrPut(url) {
      mm.nativeFetch(url).let { response ->
        if (response.status.code >= 400) {
          ImageState.Error
        } else {
          ImageState.Success(response.body.payload)
        }
      }
    } as ImageState<out Any?>
  }
}

/**
 * 图标渲染
 */
@Composable
fun WindowController.IconRender(
  modifier: Modifier = Modifier,
  primaryColor: Color = LocalContentColor.current,
  primaryContainerColor: Color? = null,
) {
  val iconUrl by watchedState { iconUrl }

  val iconMaskable by watchedState { iconMaskable }
  val iconMonochrome by watchedState { iconMonochrome }
  AppIcon(icon = iconUrl,
    modifier,
    color = primaryColor,
    containerColor = primaryContainerColor,
    iconMonochrome = iconMonochrome,
    iconMaskable = iconMaskable,
    iconFetchHook = state.constants.microModule?.let { mm ->
      return@let { request: FetchRequest, returnBlock: suspend (FetchResponse) -> Unit ->
        val response = mm.nativeFetch(request.url)
        returnBlock(response)
      }
    })
}

sealed class ImageState<T>(val model: T) {
  data object Loading : ImageState<Any?>(null)
  class Success<T>(model: T) : ImageState<T>(model)
  data object Error : ImageState<Any?>(null)
}
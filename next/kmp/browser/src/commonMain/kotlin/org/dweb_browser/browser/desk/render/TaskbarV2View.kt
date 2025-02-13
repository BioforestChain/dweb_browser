package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import org.dweb_browser.browser.desk.TaskbarV2Controller
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.helper.clamp
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isAndroid
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.helper.platform.isIOS
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.floatBar.DraggableDelegate
import org.dweb_browser.sys.window.floatBar.floatBarDefaultShape
import kotlin.math.min
import kotlin.math.sqrt

private const val TASKBAR_MIN_WIDTH = 42f
private const val TASKBAR_MAX_WIDTH = 54f
private const val TASKBAR_PADDING_VALUE = 6f
private const val TASKBAR_DIVIDER_HEIGHT = 8f

expect fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View

/**
 * Popup 策略
 */
private enum class PopupStrategy {
  /**
   * 禁用，用于桌面端
   * 桌面端的popup，渲染起来会有层级错误的问题
   */
  DISABLED,

  /**
   * 按需，用于IOS
   * 会有一些帧不连贯的、或者残影、或者延迟等问题、性能也比ALWAYS差一些，毕竟会导致内容完全重新渲染
   * 如果IOS修复了Popup抖动的问题，那么可以改为ALWAYS
   */
  REQUIRED,

  /**
   * 总是，用于Android
   */
  ALWAYS,
}

internal enum class TaskbarShape {
  /**
   * 沉浸式，隐藏Taskbar，显示Arrow图标，hover即可恢复正常显示，可用于 全屏视频、游戏时
   */
  IMMERSIVE,

  /**
   * 正常显示Taskbar
   */
  NORMAL,
}

abstract class ITaskbarV2View(protected val taskbarController: TaskbarV2Controller) {
  companion object {}

  val state = taskbarController.state

  @Composable
  protected fun RenderContent(
    draggableDelegate: DraggableDelegate,
    displaySize: Size,
    scrollMaskColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
  ) {
    val apps by taskbarController.appsFlow.collectAsState()
    val firstItem = apps.firstOrNull() ?: TaskbarAppModel("", icon = null, running = false)

    val appCount = apps.size

    /**
     * popup的渲染策略
     */
    val popupStrategy = remember {
      when {
        IPureViewController.isAndroid -> PopupStrategy.ALWAYS
        IPureViewController.isDesktop -> PopupStrategy.DISABLED
        IPureViewController.isIOS -> PopupStrategy.REQUIRED
        else -> PopupStrategy.DISABLED
      }
    }

    /**
     * 能否进行折叠
     *
     * 在移动设备上，使用折叠来节省屏幕空间，点击进行展开。
     */
    val canFold = when {
      popupStrategy == PopupStrategy.DISABLED -> false
      else -> when (firstItem.state.mode) {
        WindowMode.MAXIMIZE, WindowMode.FULLSCREEN -> true
        else -> false
      }
    }
    var isFocus by remember { mutableStateOf(false) }

    /**
     * 是否展开 taskbar 的 apps
     */
    val isExpanded = when {
      canFold -> !firstItem.state.visible || isFocus
      else -> true
    }
    val expandedAni = remember { Animatable(1f) }
    LaunchedEffect(isExpanded) {
      if (isExpanded) {
        expandedAni.animateTo(1f, taskbarExpandedAniSpec())
      } else {
        expandedAni.animateTo(0f, taskbarFoldAniSpec())
      }
    }

    var taskbarWidth by remember { mutableStateOf(TASKBAR_MAX_WIDTH) }
    var paddingValue by remember { mutableStateOf(TASKBAR_PADDING_VALUE) }
    var dividerHeight by remember { mutableStateOf(TASKBAR_DIVIDER_HEIGHT) }
    var appIconsExpandedHeight by remember { mutableStateOf(0f) }
    var appIconsFoldHeight by remember { mutableStateOf(0f) }
    var iconSize by remember { mutableStateOf(0f) }
    var taskbarAppsMaxHeight by remember { mutableStateOf(0f) }
    LaunchedEffect(appCount, displaySize, isExpanded) {
      taskbarWidth = clamp(
        TASKBAR_MIN_WIDTH, min(displaySize.width, displaySize.height) * 0.14f, TASKBAR_MAX_WIDTH
      )
      /// 更新taskbar宽度
      state.layoutWidth = taskbarWidth
      paddingValue = TASKBAR_PADDING_VALUE * taskbarWidth / TASKBAR_MAX_WIDTH
      dividerHeight = TASKBAR_DIVIDER_HEIGHT * taskbarWidth / TASKBAR_MAX_WIDTH
      iconSize = taskbarWidth - paddingValue
      if (appCount == 0) {
        appIconsExpandedHeight = 0f
        appIconsFoldHeight = 0f
      } else {
        appIconsExpandedHeight = appCount * iconSize + paddingValue
        appIconsFoldHeight = taskbarWidth + appCount
      }
      taskbarAppsMaxHeight = displaySize.height - taskbarWidth - paddingValue
    }

    val taskBarHomeButton = rememberTaskBarHomeButton(taskbarController.deskNMM)
    LaunchedEffect(draggableDelegate, taskBarHomeButton) {
      draggableDelegate.dragCallbacks += "free taskbar-home-button" to {
        taskBarHomeButton.isPressed = false
      }
    }

    Layout(modifier = modifier, content = {
      val taskbarWidthDp = taskbarWidth.dp
      Column(
        modifier = Modifier.requiredWidth(taskbarWidthDp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val taskbarAppsHeight = lerp(appIconsFoldHeight, appIconsExpandedHeight, expandedAni.value)
        val taskbarAppsHeightDp = taskbarAppsHeight.dp

        val taskbarAppsMaxHeightDp = taskbarAppsMaxHeight.dp


        @Composable
        fun AppsScrollBox(content: @Composable BoxScope.() -> Unit) {
          val appsScrollState = rememberScrollState()
          Box(
            Modifier.requiredSize(taskbarWidthDp, min(taskbarAppsMaxHeight, taskbarAppsHeight).dp)
              .clip(
                floatBarDefaultShape.copy(
                  bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)
                )
              )
          ) {
            Box(
              Modifier.requiredWidth(taskbarWidthDp).requiredHeightIn(max = taskbarAppsMaxHeightDp)
                .verticalScroll(appsScrollState)
            ) {
              Box(
                modifier = Modifier.requiredSize(taskbarWidthDp, taskbarAppsHeightDp),
                contentAlignment = Alignment.TopCenter,
                content = content,
              )
            }
            val d = LocalDensity.current.density
            val scrollY = appsScrollState.value / d
            val scrollMaskHeight = paddingValue * 2
            val scrollMaskHeightDp = scrollMaskHeight.dp
            if (scrollY > 0f) {
              Box(
                Modifier.fillMaxWidth().height(scrollMaskHeightDp).offset(
                  y = lerp(
                    -scrollMaskHeight, 0f, (scrollY / (scrollMaskHeight * 2)).fastCoerceIn(0f, 1f)
                  ).dp
                ).background(Brush.verticalGradient(listOf(scrollMaskColor, Color.Transparent)))
              )
            }
            if (taskbarAppsHeight > taskbarAppsMaxHeight) {
              val maxScrollY = appsScrollState.maxValue / d
              Box(
                Modifier.fillMaxWidth().height(scrollMaskHeightDp).offset(
                  y = lerp(
                    taskbarAppsMaxHeight - scrollMaskHeight,
                    taskbarAppsMaxHeight,
                    (1 - (maxScrollY - scrollY) / (scrollMaskHeight * 2)).fastCoerceIn(0f, 1f)
                  ).dp
                ).background(Brush.verticalGradient(listOf(Color.Transparent, scrollMaskColor)))
              )
            }
          }
        }

        @Composable
        fun <T> itemsIndexed(
          list: List<T>, getKey: (Int, T) -> Any?, action: @Composable (index: Int, T) -> Unit,
        ) {
          list.forEachIndexed { index, item ->
            key(getKey(index, item)) {
              action(index, item)
            }
          }
        }

        @Composable
        fun AppsRender(popupOffset: IntOffset? = null) {
          itemsIndexed(
            apps,
            getKey = { _, it -> it.mmid },
          ) { index, app ->
            val aniProp = app.rememberAniProp()
            aniProp.setOffsetY(lerp(index.toFloat(), iconSize * index, expandedAni.value))
            /// 让动画计算
            aniProp.Effect()

            @Suppress("DeferredResultUnused") TaskBarAppIcon(
              app = app,
              microModule = taskbarController.deskNMM,
              openAppOrActivate = {
                if (canFold) {
                  isFocus = true
                }

                app.openingFlow.value = true
                taskbarController.openAppOrActivate(app.mmid).invokeOnCompletion {
                  app.openingFlow.value = false
                }
              },
              quitApp = {
                taskbarController.closeApp(app.mmid)
              },
              toggleWindow = {
                taskbarController.toggleWindowMaximize(app.mmid)
              },
              modifier = Modifier.zIndex(apps.size - index - 1f).offset(y = aniProp.offsetYDp)
                .requiredSize(taskbarWidth.dp).padding(horizontal = paddingValue.dp),
              containerAlpha = when {
                isExpanded -> null
                else -> 1f
              },
              shadow = when {
                isExpanded -> null
                else -> 1.dp
              },
              popupOffset = popupOffset,
            )
          }
        }

        /// 修改图层的背景颜色
        LaunchedEffect(isFocus) {
          taskbarController.state.backgroundAlphaGetter = when {
            isFocus -> ({ sqrt(it) })
            else -> null
          }
        }

        when {
          popupStrategy == PopupStrategy.DISABLED || (popupStrategy == PopupStrategy.REQUIRED && !isFocus) -> AppsScrollBox { AppsRender() }

          else -> {
            /// 这里因为是一个新的Popup，所以需要手动记录一下位置，为内部长按的popup提供位置
            var popupPos by remember { mutableStateOf(IntOffset.Zero) }
            val density = LocalDensity.current.density
            Box(Modifier.onGloballyPositioned {
              popupPos = it.boundsInRoot().run {
                IntOffset(
                  x = (left + taskbarController.state.layoutX * density).toInt(),
                  y = (top + taskbarController.state.layoutY * density).toInt(),
                )
              }
            })

            /// 这里Popup需要长期存在，否则如果开关popup，会导致popup渲染残影
            Box(
              Modifier.requiredSize(taskbarWidthDp, min(taskbarAppsMaxHeight, taskbarAppsHeight).dp)
            ) {
              Popup(
                onDismissRequest = {
                  isFocus = false
                }, properties = PopupProperties(
//                focusable = isFocus,
                  dismissOnBackPress = true,
                  dismissOnClickOutside = true,
                  clippingEnabled = true,
                )
              ) {
                AppsScrollBox {
                  AppsRender(if (IPureViewController.isAndroid) popupPos else null)
                }
              }
            }
          }
        }

        /// IOS平台上，popup居然回因为外部scale导致位置重新计算
        if (IPureViewController.isIOS) {
          @Composable
          fun FixIosPopupPosition(initScale: Float) {
            var scale by remember { mutableStateOf(initScale) }
            LaunchedEffect(scale) {
              scale = when (scale) {
                1f -> 2f
                else -> 1f
              }
            }
            /// 发现每次切回1的时候，它就正常一次？
            Box(Modifier.scale(scale).size(10.dp, 1.dp))
          }
          /// 所以这里交叉切换，来强行让popup每一帧都在刷新位置
          FixIosPopupPosition(1f)
          FixIosPopupPosition(2f)
        }

        /// apps 和 底部按钮的分割线
        if (appCount > 0) {
          TaskBarDivider(Modifier.padding(horizontal = paddingValue.dp))
        }

        /// 底部按钮
        taskBarHomeButton.Render({
          taskbarController.toggleDesktopView()
        }, Modifier.padding(paddingValue.dp).zIndex(apps.size.toFloat()))
      }
    }) { measurables, constraints ->
      val placeables = measurables.map { measurable ->
        measurable.measure(constraints.copy(maxHeight = Int.MAX_VALUE, maxWidth = Int.MAX_VALUE))
      }
      val layoutWidth = placeables.maxOf { it.width }
      val layoutHeight = placeables.maxOf { it.height }
      taskbarController.state.layoutWidth = layoutWidth / density
      taskbarController.state.layoutHeight = layoutHeight / density
      layout(layoutWidth, constraints.maxHeight) {
        for (placeable in placeables) {
          placeable.place(0, 0)
        }
      }
    }
  }

  @Composable
  abstract fun Render()
}


private fun <T> taskbarExpandedAniSpec() =
  spring<T>(Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

private fun <T> taskbarFoldAniSpec() =
  spring<T>(Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

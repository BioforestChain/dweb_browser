package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import kotlin.math.min
import kotlin.math.sqrt

private const val TASKBAR_MIN_WIDTH = 32f
private const val TASKBAR_MAX_WIDTH = 54f
private const val TASKBAR_PADDING_VALUE = 6f
private const val TASKBAR_DIVIDER_HEIGHT = 8f

expect fun ITaskbarV2View.Companion.create(taskbarController: TaskbarV2Controller): ITaskbarV2View

abstract class ITaskbarV2View(protected val taskbarController: TaskbarV2Controller) {
  companion object {}

  val state = taskbarController.state

  @Composable
  protected fun RenderContent(
    draggableDelegate: DraggableDelegate,
    displaySize: Size,
    modifier: Modifier = Modifier,
  ) {
    val apps by taskbarController.appsFlow.collectAsState()
    val firstItem = apps.firstOrNull() ?: TaskbarAppModel("", icon = null, running = false)

    val appCount = apps.size

    /**
     * 桌面端默认不开启折叠，渲染起来会有问题
     */
    val disablePopup = IPureViewController.isDesktop

    /**
     * 能否进行折叠
     *
     * 在移动设备上，使用折叠来节省屏幕空间，点击进行展开。
     */
    val canFold = when {
      disablePopup -> false
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
    LaunchedEffect(appCount, displaySize, isExpanded) {
      taskbarWidth = clamp(
        TASKBAR_MIN_WIDTH, min(displaySize.width, displaySize.height) * 0.14f, TASKBAR_MAX_WIDTH
      )
      paddingValue = TASKBAR_PADDING_VALUE * taskbarWidth / TASKBAR_MAX_WIDTH
      dividerHeight = TASKBAR_DIVIDER_HEIGHT * taskbarWidth / TASKBAR_MAX_WIDTH
      iconSize = taskbarWidth - paddingValue
      if (appCount == 0) {
        appIconsExpandedHeight = 0f
        appIconsFoldHeight = 0f
      } else {
        appIconsExpandedHeight = appCount * iconSize
        appIconsFoldHeight = taskbarWidth
      }
    }

    val taskBarHomeButton = rememberTaskBarHomeButton(taskbarController.deskNMM)
    LaunchedEffect(draggableDelegate, taskBarHomeButton) {
      draggableDelegate.dragCallbacks += "free taskbar-home-button" to {
        taskBarHomeButton.isPressed = false
      }
    }

    Layout(modifier = modifier, content = {
      Column(
        modifier = Modifier.requiredWidth(taskbarWidth.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val taskbarAppsHeightDp =
          lerp(appIconsFoldHeight, appIconsExpandedHeight, expandedAni.value).dp

        Box(
          modifier = modifier.fillMaxSize().requiredHeight(taskbarAppsHeightDp),
          contentAlignment = Alignment.TopCenter,
        ) {
          /// 让动画计算
          apps.forEach { app -> app.aniProp.Effect() }

          @Composable
          fun <T> itemsIndexed(
            list: List<T>, key: (Int, T) -> Any?, item: @Composable (index: Int, T) -> Unit,
          ) {
            list.forEachIndexed { index, item ->
              key(key(index, item)) {
                item(index, item)
              }
            }
          }

          @Composable
          fun appsRender(popupOffset: IntOffset? = null) {
            itemsIndexed(
              apps,
              key = { _, it -> it.mmid },
            ) { index, app ->
              val aniProp = app.aniProp
              aniProp.setOffsetY(lerp(index.toFloat(), iconSize * index, expandedAni.value))

              @Suppress("DeferredResultUnused") TaskBarAppIcon(
                app = app,
                microModule = taskbarController.deskNMM,
                openAppOrActivate = {
                  if (canFold) {
                    isFocus = true
                  }

                  app.opening = true
                  taskbarController.openAppOrActivate(app.mmid).invokeOnCompletion {
                    app.opening = false
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

          when {
            disablePopup -> appsRender()

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
              /// 修改图层的背景颜色
              LaunchedEffect(isFocus) {
                taskbarController.state.backgroundAlphaGetter = if (isFocus) {
                  { sqrt(it) }
                } else {
                  null
                }
              }

              /// 这里Popup需要长期存在，否则如果开关popup，会导致popup渲染残影
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
                Box(Modifier.height(taskbarAppsHeightDp)) {
                  appsRender(if (IPureViewController.isAndroid) popupPos else null)
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
          TaskBarDivider(
            paddingValue.dp,
            when {
              IPureViewController.isIOS -> (paddingValue - 2).dp
              else -> paddingValue.dp
            }
          )
        }

        /// 底部按钮
        taskBarHomeButton.Render({
          taskbarController.toggleDesktopView()
        }, Modifier.padding(paddingValue.dp).zIndex(apps.size.toFloat()))
      }
    }) { measurables, constraints ->
      val placeables = measurables.map { measurable ->
        measurable.measure(constraints.copy(maxHeight = Int.MAX_VALUE))
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

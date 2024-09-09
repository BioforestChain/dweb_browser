package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityStyle
import org.dweb_browser.browser.desk.model.rememberActivityStyle
import org.dweb_browser.helper.compose.rememberMultiGraphicsLayers
import org.dweb_browser.helper.compose.toAwtColor
import org.dweb_browser.helper.platform.PureViewController
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog

internal class ActivityViewController(
  val controller: ActivityController,
  parentWindow: ComposeWindow,
) {
  var offsetY by mutableStateOf(0f)
  var activityStyle = ActivityStyle()
  val devParams = ActivityControllerDevParams().apply {
    if (PureViewController.isWindows) {
      resizePolicy = ActivityControllerDevParams.ResizePolicy.Enum.ReduceResize
    } else if (PureViewController.isMacOS) {
      resizePolicy = ActivityControllerDevParams.ResizePolicy.Enum.LazyResize()
    }
  }
  val composePanel = ComposePanel()
  val dialog = JDialog()

  // 根据父级窗口所在的屏幕来获取toolkit，目前虽然等价于 Toolkit.getDefaultToolkit，但是有望未来升级？
  val displaySize = parentWindow.toolkit.screenSize

  init {
    composePanel.setContent {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Render()
      }
    }

    composePanel.background = java.awt.Color(0, 0, 0, 0)
    composePanel.isOpaque = false
    dialog.apply {
      isFocusable = false
      isAlwaysOnTop = true
      isUndecorated = true
      background = java.awt.Color(0, 0, 0, 0)
      add(composePanel)
    }
  }

  private fun setDialogSize(width: Int, height: Int, y: Int = 0) {
    dialog.size = Dimension(width, height)
    // 窗口相对屏幕居中显示
    dialog.location = Point((displaySize.width - width) / 2, y)
  }

  @Composable
  private fun Render() {
    val uiScope = rememberCoroutineScope()
    var dialogWidth by remember { mutableIntStateOf(dialog.size.width) }
    var dialogHeight by remember { mutableIntStateOf(dialog.size.height) }
    Layout(modifier = Modifier, content = {
      val graphicsLayers = rememberMultiGraphicsLayers(devParams.preFrames)
      var frameIndex by remember { mutableStateOf(0) }
      fun getGraphicsLayer(index: Int) = graphicsLayers[index % graphicsLayers.size]
      Box(
        Modifier.drawWithContent {
          val graphicsLayer = getGraphicsLayer(++frameIndex)
          val preGraphicsLayer = getGraphicsLayer(frameIndex + 1)
          graphicsLayer.record {
            this@drawWithContent.drawContent()
          }
          if (devParams.usePreFrame) {
            val preSize = preGraphicsLayer.size
            val newSize = graphicsLayer.size
            val diffX = (newSize.width - preSize.width).toFloat()
            val diffY = (newSize.height - preSize.height).toFloat()
            val tx: Float
            val ty: Float

            when (devParams.useTranslateMode) {
              ActivityControllerDevParams.TranslateMode.Auto -> {
                /// 如果图像在放大，那么使用居中定位，否则使用左上角定位。
                /// 别问我为什么，这是实验得出来的结论。
                /// 但大概的原因是根 UIView 放置 ComposeView 的行为有关，主要是因为
                tx = if (diffX > 0) diffX / 2 else 0f
                ty = if (diffY > 0) diffY / 2 else 0f
              }

              ActivityControllerDevParams.TranslateMode.TopStart -> {
                tx = 0f
                ty = 0f
              }

              ActivityControllerDevParams.TranslateMode.Center -> {
                tx = diffX / 2
                ty = diffY / 2
              }

              ActivityControllerDevParams.TranslateMode.EndBottom -> {
                tx = diffX
                ty = diffY
              }
            }
            translate(tx, ty) {
              drawLayer(preGraphicsLayer)
            }
          } else {
            drawLayer(graphicsLayer)
          }
        },
        contentAlignment = Alignment.Center,
      ) {
        CommonActivityListRender(controller, activityStyle)
      }
    }) { measurables, constraints ->
      fun Int.toDp() = (this / density)
      fun Int.toPx() = (this * density).fastRoundToInt()
      fun Float.toDp() = (this / density)
      fun Float.toPx() = (this * density).fastRoundToInt()
      // MEASUREMENT SCOPE
      val placeables = measurables.map { measurable ->
        measurable.measure(
          constraints.copy(
            maxWidth = (displaySize.width * density).toInt(),
            maxHeight = (displaySize.height * density).toInt(),
          )
        )
      }
      if (placeables.isEmpty()) {
        return@Layout layout(constraints.maxWidth, constraints.maxHeight) {}
      }
      val layoutWidth = placeables.maxOf { it.width }
      val layoutHeight = placeables.maxOf { it.height }
      dialogWidth = layoutWidth.toDp().coerceAtLeast(1f).fastRoundToInt()
      dialogHeight = layoutHeight.toDp().coerceAtLeast(1f).fastRoundToInt()

      when (val resizePolicy = devParams.resizePolicy) {
        /// 延迟resize
        is ActivityControllerDevParams.ResizePolicy.Enum.LazyResize -> {
          if (resizePolicy.dirty) {
            resizePolicy.dirty = false
            setDialogSize(dialogWidth, dialogHeight)
          } else {
            resizePolicy.safePadding.apply {
              if (dialogWidth > dialog.size.width || dialogHeight > dialog.size.height) {
                setDialogSize(dialogWidth + width, dialogHeight + height)
              }
            }
          }
          if (dialogWidth != dialog.size.width || dialogHeight != dialog.size.height) {
            resizePolicy.delayDoneJob?.cancel()
            resizePolicy.delayDoneJob = uiScope.launch {
              delay(resizePolicy.sampleTime)
              resizePolicy.dirty = true
              resizePolicy.delayDoneJob = null
            }
          }
        }
        /// 立即resize
        ActivityControllerDevParams.ResizePolicy.Enum.ImmediateResize -> {
          dialog.minimumSize = Dimension(1, 1)
          setDialogSize(dialogWidth, dialogHeight, offsetY.fastRoundToInt())
        }
        /// 减少reduce
        ActivityControllerDevParams.ResizePolicy.Enum.ReduceResize -> {
          dialog.minimumSize = Dimension(dialogWidth, (dialogHeight + offsetY).fastRoundToInt())
          // 窗口相对屏幕居中显示
          dialog.location = Point((displaySize.width - dialog.size.width) / 2, 0)
        }
        /// 自定义宽高
        is ActivityControllerDevParams.ResizePolicy.Enum.CustomResize -> {
          dialog.minimumSize = Dimension(1, 1)
          setDialogSize(resizePolicy.width, resizePolicy.height)
        }
      }

      layout(layoutWidth, layoutHeight) {
        placeables.forEach {
          it.place(0, 0)
        }
      }
    }
    when (val resizePolicy = devParams.resizePolicy) {
      is ActivityControllerDevParams.ResizePolicy.Enum.LazyResize -> {
        if (resizePolicy.dirty) {
          resizePolicy.dirty = false
          setDialogSize(dialogWidth, dialogHeight)
        }
      }

      else -> {}
    }
  }

  @Composable
  fun Launch() {
    /// 自定义样式
    activityStyle = rememberActivityStyle(remember {
      {
        copy(
          containerBox = { content ->
            if (devParams.resizePolicy == ActivityControllerDevParams.ResizePolicy.Enum.ReduceResize) {
              Box(
                Modifier.offset(y = offsetDp),
                content = content,
              )
            } else {
              offsetY = offsetDp.value
              Box(content = content)
            }
          },
          contentBox = { content ->
            Box(
              modifier,
              contentAlignment = Alignment.Center,
              content = content,
            )
          },
          openShadowElevation = 0f,
        )
      }
    })
    /// 避免窗口被强行关闭
    val isVisible by produceState(dialog.isVisible) {
      dialog.addComponentListener(object : ComponentAdapter() {
        override fun componentShown(e: ComponentEvent?) {
          value = true
        }

        override fun componentHidden(e: ComponentEvent?) {
          value = false
        }
      })
    }
    LaunchedEffect(isVisible) {
      if (dialog.size.width * dialog.size.height == 0) {
        /// 默认给定一个常见的大小
        dialog.size = Dimension(DIALOG_COMMON_WIDTH, DIALOG_COMMON_HEIGHT)
      }
      dialog.isVisible = true
    }
    if (DEV_ACTIVITY_CONTROLLER) {
      LaunchedEffect(devParams.showDevLayer) {
        if (dialog.isUndecorated) {
          dialog.background = when {
            devParams.showDevLayer -> Color.Blue.copy(alpha = 0.27f)
            else -> Color.Transparent
          }.toAwtColor()
        }
      }
    }
  }
}

private const val DIALOG_COMMON_WIDTH = 480
private const val DIALOG_COMMON_HEIGHT = 220
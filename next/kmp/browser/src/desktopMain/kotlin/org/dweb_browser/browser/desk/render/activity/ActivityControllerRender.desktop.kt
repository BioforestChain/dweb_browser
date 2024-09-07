package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityStyle
import org.dweb_browser.browser.desk.model.rememberActivityStyle
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.rememberMultiGraphicsLayers
import org.dweb_browser.helper.compose.toAwtColor
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.asDesktop
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog

@Composable
actual fun ActivityController.Render() {
  val avc = activityControllerPvcWM.getOrPut(this) {
    ActivityViewController(
      this,
      LocalPureViewController.current.asDesktop().composeWindowAsState().value
    )
  }
  avc.activityStyle = rememberActivityStyle(remember {
    {
      copy(
        containerBox = { content ->
          if (avc.devParams.reduceResize) {
            Box(
              Modifier.offset(y = offsetDp),
              content = content,
            )
          } else {
            avc.offsetY = offsetDp.value
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

  avc.Launch()
  /// 调试面板
  if (DEV_ACTIVITY_CONTROLLER) {
    Box(Modifier.fillMaxSize()) {
      val devParams = avc.devParams
      Column(
        Modifier.align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp)
      ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("打开图层辅助")
          Switch(devParams.showDevLayer, { devParams.showDevLayer = it })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("使用错帧显示")
          Switch(devParams.usePreFrame, { devParams.usePreFrame = it })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("减少setSize的发生")
          Switch(devParams.reduceResize, { devParams.reduceResize = it })
        }
        SingleChoiceSegmentedButtonRow {
          DevParams.TranslateMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
              devParams.useTranslateMode == mode, { devParams.useTranslateMode = mode },
              shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = DevParams.TranslateMode.entries.size
              ),
              label = { Text(mode.name) },
              enabled = false,
            )
          }
        }
        ActivityDevPanel(this@Render)
      }
    }
  }
}

private class ActivityViewController(
  val controller: ActivityController,
  parentWindow: ComposeWindow
) {
  var offsetY by mutableStateOf(0f)
  var activityStyle = ActivityStyle()
  val devParams = DevParams().apply {
    reduceResize = PureViewController.isWindows
  }
  val composePanel = ComposePanel()
  val dialog = JDialog()

  // 根据父级窗口所在的屏幕来获取toolkit，目前虽然等价于 Toolkit.getDefaultToolkit，但是有望未来升级？
  val displaySize = parentWindow.toolkit.screenSize

  init {
    composePanel.setContent {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Layout(modifier = Modifier, content = {
          val graphicsLayers = rememberMultiGraphicsLayers(2)
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
                  DevParams.TranslateMode.Auto -> {
                    /// 如果图像在放大，那么使用居中定位，否则使用左上角定位。
                    /// 别问我为什么，这是实验得出来的结论。
                    /// 但大概的原因是根 UIView 放置 ComposeView 的行为有关，主要是因为
                    tx = if (diffX > 0) diffX / 2 else 0f
                    ty = if (diffY > 0) diffY / 2 else 0f
                  }

                  DevParams.TranslateMode.TopStart -> {
                    tx = 0f
                    ty = 0f
                  }

                  DevParams.TranslateMode.Center -> {
                    tx = diffX / 2
                    ty = diffY / 2
                  }

                  DevParams.TranslateMode.EndBottom -> {
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
          val dialogWidth = layoutWidth.toDp().coerceAtLeast(1f).fastRoundToInt()
          val dialogHeight = layoutHeight.toDp().coerceAtLeast(1f).fastRoundToInt()
          if (devParams.reduceResize) {
            dialog.minimumSize = Dimension(dialogWidth, (dialogHeight + offsetY).fastRoundToInt())
            // 窗口相对屏幕居中显示
            dialog.location =
              Point((displaySize.width - dialog.size.width) / 2, 0)
          } else {
            dialog.size = Dimension(dialogWidth, dialogHeight)
            // 窗口相对屏幕居中显示
            dialog.location =
              Point((displaySize.width - dialog.size.width) / 2, offsetY.fastRoundToInt())
          }


          layout(layoutWidth, layoutHeight) {
            placeables.forEach {
              // 因为 setBoundsInMain 不会立刻生效，所以这里需要先移动到新的位置
//              it.place(((dialog.width.toPx() - layoutWidth) / 2), 0)
              it.place(0, 0)
            }
          }
        }
      }
    }

    composePanel.background = java.awt.Color(0, 0, 0, 0)
    dialog.apply {
      isFocusable = false
      isAlwaysOnTop = true
      isUndecorated = true
      background = java.awt.Color(0, 0, 0, 0)
      add(composePanel)
    }
  }

  @Composable
  fun Launch() {
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
    println("QAQ isVisible=$isVisible")
    LaunchedEffect(isVisible) {
      if (dialog.size.width * dialog.size.height == 0) {
        /// 默认给定一个常见的大小
        dialog.size = Dimension(DIALOG_COMMON_WIDTH, DIALOG_COMMON_HEIGHT)
      }
      dialog.isVisible = true
    }
    if (DEV_ACTIVITY_CONTROLLER) {
      LaunchedEffect(devParams.showDevLayer) {
        dialog.background = when {
          devParams.showDevLayer -> Color.Blue.copy(alpha = 0.27f)
          else -> Color.Transparent
        }.toAwtColor()
      }
    }
  }
}
private const val DIALOG_COMMON_WIDTH = 480
private const val DIALOG_COMMON_HEIGHT = 220

private class DevParams {
  var showDevLayer by mutableStateOf(false)
  var usePreFrame by mutableStateOf(false)
  var useTranslateMode by mutableStateOf(TranslateMode.Auto)

  /**
   * 是否减少 resize 的发生
   * 在windows上，最好不好进行resize，resize会导致视图消失，开关双缓冲也没用，目前无解，所以只能给一个尽可能大的空间来绘制
   * 但是，好在windows上，透明区域默认是点击穿透的
   */
  var reduceResize by mutableStateOf(false)

  enum class TranslateMode {
    Auto,
    TopStart,
    Center,
    EndBottom,
  }
}

private val activityControllerPvcWM = WeakHashMap<ActivityController, ActivityViewController>()


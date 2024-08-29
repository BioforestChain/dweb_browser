package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastJoinToString
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityStyle
import org.dweb_browser.browser.desk.model.rememberActivityStyle
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.rememberMultiGraphicsLayers
import org.dweb_browser.helper.compose.toUIColor
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.rememberDisplaySize
import platform.UIKit.UIColor

@Composable
actual fun ActivityController.Render() {
  val avc = activityControllerPvcWM.getOrPut(this) {
    ActivityViewController(this)
  }
  avc.activityStyle = rememberActivityStyle(remember {
    {
      copy(
        containerBox = { content ->
          avc.offsetY = offsetDp.value
          // pvc.setBoundsInMain(pvc.getBounds().copy(y = offsetY))
          Box(content = content)
        },
        contentBox = { content ->
          Box(
            modifier,
            contentAlignment = Alignment.Center,
            content = content,
          )
        }
      )
    }
  })

  avc.Launch()

  if (DEV_ACTIVITY_CONTROLLER) {
    Box(Modifier.fillMaxSize()) {
      val devParams = avc.devParams
      Column(
        Modifier.align(Alignment.BottomCenter)
          .padding(WindowInsets.safeGestures.asPaddingValues())
      ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("打开图层辅助")
          Switch(devParams.showDevLayer, { devParams.showDevLayer = it })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
          Text("使用错帧显示")
          Switch(devParams.usePreFrame, { devParams.usePreFrame = it })
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
              enabled = devParams.usePreFrame,
            )
          }
          SingleChoiceSegmentedButtonRow { }
        }
        ActivityDevPanel(this@Render)
      }
    }
  }
}

private class ActivityViewController(val controller: ActivityController) {
  val pvc = PureViewController(fullscreen = false)
  var offsetY by mutableStateOf(0f)
  var activityStyle = ActivityStyle()

  val devParams = DevParams()

  init {
    pvc.addContent {
      val displaySize = rememberDisplaySize()
      Layout(content = {
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
        ) {
          CommonActivityListRender(controller, activityStyle)
        }
      }) { measurables, constraints ->
        fun Float.toPx() = (this * density).toInt()
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
          pvc.setBoundsInMain(
            PureRect(0f, 0f, displaySize.width, 1f),
          )
          return@Layout layout(constraints.maxWidth, constraints.maxHeight) {}
        }
        println(
          """QAQ maxWidth = ${(displaySize.width * density).toInt()}, maxHeight = ${(displaySize.height * density).toInt()},
        """.trimIndent()
        )
        val layoutWidth = placeables.maxOf { it.width }
        val layoutHeight = placeables.maxOf { it.height }
        val boundsWidth = layoutWidth / density
        val boundsHeight = layoutHeight / density
        val newBounds = PureRect(
          x = (displaySize.width - boundsWidth) / 2,
          y = offsetY,
          width = boundsWidth,
          height = boundsHeight,
        )
        pvc.setBoundsInMain(newBounds)
        layout(layoutWidth, layoutHeight) {
          placeables.forEach {
            // 因为 setBoundsInMain 不会立刻生效，所以这里需要先移动到新的位置
            it.place(0, 0)
          }
        }
      }
    }
  }

  @Composable
  fun Launch() {
    val displaySize = rememberDisplaySize()
    LaunchedEffect(pvc) {
      nativeViewController.addOrUpdate(pvc, Int.MAX_VALUE - 100)
      /// 需要给一个初始化的bounds，否则compose默认处于一个0x0的区域，是不会触发渲染的
      pvc.setBounds(PureRect(0f, 0f, displaySize.width, 1f))
    }
    if (DEV_ACTIVITY_CONTROLLER) {
      LaunchedEffect(devParams.showDevLayer) {
        pvc.uiViewControllerInMain.view.backgroundColor = when {
          devParams.showDevLayer -> Color.Blue.copy(alpha = 0.27f).toUIColor()
          else -> UIColor.clearColor
        }
      }
    }
  }
}

private class DevParams {
  var showDevLayer by mutableStateOf(false)
  var usePreFrame by mutableStateOf(true)
  var useTranslateMode by mutableStateOf(TranslateMode.Auto)

  enum class TranslateMode {
    Auto,
    TopStart,
    Center,
    EndBottom,
  }
}

private val activityControllerPvcWM = WeakHashMap<ActivityController, ActivityViewController>()

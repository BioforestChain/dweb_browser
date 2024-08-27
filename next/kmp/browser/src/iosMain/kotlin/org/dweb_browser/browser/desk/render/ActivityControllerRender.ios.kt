package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastJoinToString
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.rememberActivityStyle
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.toUIColor
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.rememberDisplaySize
import kotlin.math.ceil

val devActivityController = false

@Composable
actual fun ActivityController.Render() {
  val avc = activityControllerPvcWM.getOrPut(this) {
    ActivityViewController(this)
  }

  avc.Launch()

  if (devActivityController) {
    Box(Modifier.fillMaxSize()) {
      ActivityDevPanel(
        this@Render,
        Modifier.align(Alignment.BottomCenter).padding(WindowInsets.safeGestures.asPaddingValues())
      )
    }
  }
}

private class ActivityViewController(val controller: ActivityController) {
  val pvc = PureViewController(fullscreen = false)

  init {
    pvc.addContent {
      val activityStyle = rememberActivityStyle()
      val displaySize = rememberDisplaySize()

      Layout(content = {
//        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        CommonActivityListRender(controller, activityStyle)
//        }
      }) { measurables, constraints ->
        println("QAQ constraints=$constraints")
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
          println("QAQ placeables is empty")
          return@Layout layout(constraints.maxWidth, constraints.maxHeight) {
            pvc.setBoundsInMain(PureRect(0f, 0f, 1f, 1f), pvc.uiViewControllerInMain.view)
          }
        }
        println(
          """QAQ maxWidth = ${(displaySize.width * density).toInt()}, maxHeight = ${(displaySize.height * density).toInt()},
        """.trimIndent()
        )
        val layoutWidth = placeables.maxOf { it.width }
        val layoutHeight = placeables.maxOf { it.height }
        val boundsPadding = 4f
        val boundsWidth = ceil(layoutWidth / density) + boundsPadding + boundsPadding
        val boundsHeight = ceil(layoutHeight / density) + boundsPadding + boundsPadding
        pvc.setBoundsInMain(
          PureRect(
            (displaySize.width - boundsWidth) / 2,
            0f,
            boundsWidth,
            boundsHeight,
          ), pvc.uiViewControllerInMain.view
        )
        println("QAQ layoutWidth=$layoutWidth layoutHeight=$layoutHeight")
        println("QAQ placeables=${placeables.fastJoinToString { "${it.width}x${it.height}" }}")
        layout(layoutWidth, layoutHeight) {
          placeables.forEach {
            it.place((boundsPadding * density).toInt(), (boundsPadding * density).toInt())
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
      if (devActivityController) {
        pvc.uiViewControllerInMain.view.backgroundColor = Color.Blue.copy(alpha = 0.1f).toUIColor()
      }
    }
  }
}

private val activityControllerPvcWM = WeakHashMap<ActivityController, ActivityViewController>()

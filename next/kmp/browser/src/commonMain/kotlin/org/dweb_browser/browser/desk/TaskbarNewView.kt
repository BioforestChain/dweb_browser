package org.dweb_browser.browser.desk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.imageFetchHook


@Composable
fun NewDesktopView(taskbarController: TaskbarController) {

}

private val paddingValue = 8
private val taskBarWidth = 60f
private val taskBarDividerHeight = 10f

@Composable
fun NewTaskbarView(
  taskbarController: TaskbarController,
  draggableHelper: ITaskbarView.DraggableHelper,
  modifier: Modifier
) {

  val apps = remember { mutableStateListOf<TaskbarAppModel>() }
  val scope = rememberCoroutineScope()

  fun updateTaskBarSize(appCount: Int) {
    taskbarController.state.layoutWidth = taskBarWidth
    taskbarController.state.layoutHeight = when (appCount) {
      0 -> taskBarWidth
      else -> appCount * (taskBarWidth - paddingValue) + taskBarDividerHeight + taskBarWidth
    }
  }

  LaunchedEffect(Unit) {
    val taskbarApps = taskbarController.getTaskbarAppList(Int.MAX_VALUE).map {
      TaskbarAppModel(it.mmid, it.icons.firstOrNull()?.src ?: "", it.running)
    }
    apps.clear()
    apps.addAll(taskbarApps)
    updateTaskBarSize(taskbarApps.count())
  }

  Box(
    modifier
      .pointerInput(draggableHelper) {
        detectDragGestures(
          onDragEnd = draggableHelper.onDragEnd,
          onDragCancel = draggableHelper.onDragEnd,
          onDragStart = draggableHelper.onDragStart,
          onDrag = { _, dragAmount ->
            draggableHelper.onDrag(dragAmount.div(density))
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      apps.forEach {
        TaskBarAppIcon(
          Modifier,
          it,
          taskbarController.deskNMM.imageFetchHook,
          click = {
            scope.launch {
              taskbarController.open(it)
            }
          }, close = {
            scope.launch {
              taskbarController.quit(it)
            }
          }
        )
      }

      if (apps.isNotEmpty()) {
        TaskBarDivider()
      }

      TaskBarHomeIcon(){
        scope.launch {
          taskbarController.toggleDesktopView()
        }
      }
    }
  }

}

@Composable
private fun TaskBarAppIcon(
  modifier: Modifier,
  app: TaskbarAppModel,
  hook: FetchHook,
  click: (mmid: String) -> Unit,
  close: (mmid: String) -> Unit
) {

  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  var showClose by remember { mutableStateOf(false) }

  LaunchedEffect(app.mmid, app.running) {
    showClose = false
  }

  fun doClickAnimation() {
    scope.launch {
      scaleValue.animateTo(1.1f)
      scaleValue.animateTo(1.0f)
    }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
    .graphicsLayer {
      scaleX = scaleValue.value
      scaleY = scaleValue.value
    }
    .padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
    .aspectRatio(1.0f)
    .background(Color.White, RoundedCornerShape(12.dp))
    .pointerInput(Unit) {
      detectTapGestures(
        onPress = {
          doClickAnimation()
        },
        onTap = {
          click(app.mmid)
        },
        onLongPress = {
          showClose = true
        }
      )
    }) {
    AppIcon(
      app.icon,
      iconFetchHook = hook,
      modifier = Modifier.blur(if (showClose) 1.dp else 0.dp)
    )

    AnimatedVisibility(showClose,
      Modifier
        .fillMaxSize()
        .clickable {
          close(app.mmid)
          showClose = false
      },
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2.0f * 0.8f

        val xLeft = (size.width - 2 * r) / 2f + r * 0.7f
        val xRight = size.width - (size.width - 2 * r) / 2f - r * 0.7f
        val yTop = (size.height - 2 * r) / 2f + r * 0.7f
        val yBottom = size.height - (size.height - 2 * r) / 2f - r * 0.7f

        drawCircle(Color.Black, r, style = Stroke(width = 5f))

        drawLine(
          Color.Gray,
          Offset(xLeft, yTop),
          Offset(xRight, yBottom),
          5f
        )

        drawLine(
          Color.Gray,
          Offset(xLeft, yBottom),
          Offset(xRight, yTop),
          5f
        )
      }
    }
  }
}

@Composable
private fun TaskBarDivider() {
  Divider(
    Modifier
      .padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
      .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black, Color.Transparent)))
    ,
    color = Color.Transparent
  )
}

@Composable
private fun TaskBarHomeIcon(click: () -> Unit) {
  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()

  fun doClickAnimation() {
    scope.launch {
      scaleValue.animateTo(1.1f)
      scaleValue.animateTo(1.0f)
    }
  }

  Box(
    modifier = Modifier
      .graphicsLayer {
        scaleX = scaleValue.value
        scaleY = scaleValue.value
      }
      .aspectRatio(1.0f)
      .padding(paddingValue.dp)
      .background(Color.Blue, RoundedCornerShape(8.dp))
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            doClickAnimation()
          },
          onTap = {
            click()
          }
        )
      }
  ) {

  }
}

private data class TaskbarAppModel(val mmid: String, val icon: String, val running: Boolean)
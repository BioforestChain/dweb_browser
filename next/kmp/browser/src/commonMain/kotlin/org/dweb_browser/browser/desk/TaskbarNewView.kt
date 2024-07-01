package org.dweb_browser.browser.desk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.window.render.imageFetchHook
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val paddingValue = 6
private val taskBarWidth = 55f
private val taskBarDividerHeight = 8f

@Composable
fun NewTaskbarView(
  taskbarController: TaskbarController,
  draggableHelper: ITaskbarView.DraggableHelper,
  modifier: Modifier,
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

  fun doGetApps() {
    scope.launch {
      val taskbarApps = taskbarController.getTaskbarAppList(Int.MAX_VALUE).map { new ->
        val oldApp = apps.firstOrNull { old ->
          old.mmid == new.mmid && old.icon == new.icons.firstOrNull()?.src
        }
        TaskbarAppModel(
          new.mmid,
          new.icons.firstOrNull()?.src ?: "",
          new.running,
          oldApp?.isShowClose ?: false,
        )
      }
      apps.clear()
      apps.addAll(taskbarApps)
      updateTaskBarSize(taskbarApps.count())
    }
  }

  DisposableEffect(Unit) {
    val job = taskbarController.onUpdate.run {
      filter { it != "bounds" }
    }.collectIn(scope) {
      doGetApps()
    }

    onDispose {
      job.cancel()
    }
  }

  Box(
    modifier.pointerInput(draggableHelper) {
      detectDragGestures(onDragEnd = draggableHelper.onDragEnd,
        onDragCancel = draggableHelper.onDragEnd,
        onDragStart = draggableHelper.onDragStart,
        onDrag = { _, dragAmount ->
          draggableHelper.onDrag(dragAmount.div(density))
        })
    }, contentAlignment = Alignment.Center
  ) {

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      apps.forEach { app ->
        TaskBarAppIcon(Modifier,
          app,
          taskbarController.iconStore,
          taskbarController.deskNMM,
          openApp = { mmid ->
          scope.launch {
            taskbarController.open(mmid)
          }
        }, quitApp = { mmid ->
          scope.launch {
            taskbarController.quit(mmid)
          }
        }, toggleWindow = { mmid ->
          scope.launch {
            taskbarController.toggleWindowMaximize(mmid)
          }
        })
      }

      if (apps.isNotEmpty()) {
        TaskBarDivider()
      }

      TaskBarHomeIcon() {
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
  iconStore: DeskIconStore,
  microModule: NativeMicroModule.NativeRuntime,
  openApp: (mmid: String) -> Unit,
  quitApp: (mmid: String) -> Unit,
  toggleWindow: (mmid: String) -> Unit,
) {

  val scaleValue = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()
  var showQuit by remember(app.isShowClose) { mutableStateOf(app.isShowClose) }

  fun doAnimation() {
    scope.launch {
      scaleValue.animateTo(1.1f)
      scaleValue.animateTo(1.0f)
    }
  }

  BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier.graphicsLayer {
    scaleX = scaleValue.value
    scaleY = scaleValue.value
  }.padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
    .aspectRatio(1.0f).shadow(3.dp, RoundedCornerShape(12.dp))
    .background(Color.White, RoundedCornerShape(12.dp))
    .pointerInput(app) {
      detectTapGestures(onPress = {
        doAnimation()
      }, onTap = {
        openApp(app.mmid)
      }, onDoubleTap = {
        if (app.running) {
          toggleWindow(app.mmid)
        } else {
          openApp(app.mmid)
        }
      }, onLongPress = {
        if (app.running) {
          showQuit = true
        } else {
          openApp(app.mmid)
        }
      })
    }) {


    key(app.icon) {
      BoxWithConstraints(Modifier.padding(5.dp).blur(if (showQuit) 1.dp else 0.dp)) {
        val iconImage = TaskbarAppModel.getCacheIcon(app.icon)
        if (iconImage != null) {
          Image(iconImage, contentDescription = null)
        } else {
          DeskCacheIcon(app.icon, iconStore, microModule, maxWidth, maxHeight) {
            TaskbarAppModel.setCacheIcon(app.mmid, it)
          }
        }
      }
    }

    if (showQuit) {
      if (taskBarCloseButtonUsePopUp()) {
        Popup(onDismissRequest = {
          showQuit = false
        }) {
          CloseButton(Modifier.size(maxWidth).clickableWithNoEffect {
            quitApp(app.mmid)
            showQuit = false
          })
        }
      } else {
        CloseButton(Modifier.size(maxWidth).clickableWithNoEffect {
          quitApp(app.mmid)
          showQuit = false
        })
      }
    }
  }
}

@Composable
fun CloseButton(modifier: Modifier) {
  Canvas(modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape)) {
    val r = size.minDimension / 2.0f * 0.8f

    val xLeft = (size.width - 2 * r) / 2f + r * 0.7f
    val xRight = size.width - (size.width - 2 * r) / 2f - r * 0.7f
    val yTop = (size.height - 2 * r) / 2f + r * 0.7f
    val yBottom = size.height - (size.height - 2 * r) / 2f - r * 0.7f

    val lineWidth = taskBarCloseButtonLineWidth()

    drawCircle(Color.Gray, r, style = Stroke(width = lineWidth))

    drawLine(
      Color.Black, Offset(xLeft, yTop), Offset(xRight, yBottom), lineWidth
    )

    drawLine(
      Color.Black, Offset(xLeft, yBottom), Offset(xRight, yTop), lineWidth
    )
  }
}

expect fun taskBarCloseButtonLineWidth(): Float
expect fun taskBarCloseButtonUsePopUp(): Boolean

@Composable
private fun TaskBarDivider() {
  HorizontalDivider(
    Modifier.padding(start = paddingValue.dp, top = paddingValue.dp, end = paddingValue.dp)
      .background(
        Brush.horizontalGradient(
          listOf(
            Color.Transparent, Color.Black, Color.Transparent
          )
        )
      ), color = Color.Transparent
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

  BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer {
    scaleX = scaleValue.value
    scaleY = scaleValue.value
  }.aspectRatio(1.0f).padding(paddingValue.dp)) {
    desktopWallpaperView(
      5,
      modifier = Modifier.blur(1.dp, BlurredEdgeTreatment.Unbounded).clip(CircleShape)
        .shadow(3.dp, CircleShape)
    ) {
      doClickAnimation()
      click()
    }
  }
}

private data class TaskbarAppModel(
  val mmid: String,
  val icon: String,
  val running: Boolean,
  var isShowClose: Boolean = false,
) {
  companion object {
    private val iconCache = mutableMapOf<String, ImageBitmap>()
    fun getCacheIcon(mmid: String) = iconCache[mmid]
    fun setCacheIcon(mmid: String, image: ImageBitmap) {
      iconCache[mmid] = image
    }
  }
}

@Composable
fun BezGradient(color: Color, modifier: Modifier, style: DrawStyle = Fill, random: Float = 20f) {

  fun toCanvasCoordinate(point: Offset, center: Offset): Offset {
    return Offset(point.x + center.x, point.y + center.y)
  }

  fun randomPolarPoint(degree: Float, oR: Float, iR: Float): Offset {
    val oY = oR * sin(degree / 180f * PI)
    val iY = iR * sin(degree / 180f * PI)

    val oX = oR * cos(degree / 180f * PI)
    val iX = iR * cos(degree / 180f * PI)

    val randomX = Random.nextFloat()
    val randomY = Random.nextFloat()


    val x = when (degree) {
      in 0f..90f -> randomX * (oX - iX) + iX
      in 270f..360f -> randomX * (oX - iX) + iX
      else -> randomX * (iX - oX) + oX
    }.toInt()

    val y = when (degree) {
      in 0f..180f -> randomY * (oY - iY) + iY
      else -> randomY * (oY - iY) + iY
    }.toInt()

    return Offset(x.toFloat(), y.toFloat())
  }

  fun allDegress(number: Int): List<Float> {
    val result = mutableListOf<Float>()
    val step = 360f / number.toFloat()
    var i = 0
    while (i < number) {
      result.add(step * i)
      i++;
    }
    return result
  }

  fun bezierEndPoint(c0: Offset, c1: Offset): Offset {
    return Offset((c1.x - c0.x) / 2 + c0.x, (c1.y - c0.y) / 2 + c0.y)
  }

  fun getPath(center: Offset, radius: Float): Path {
    val points = allDegress(8).map {
      randomPolarPoint(it, radius.toFloat(), radius.toFloat() - random)
    }.map {
      toCanvasCoordinate(it, center)
    }

    val path = Path()

    var start = points[0]
    var c0 = Offset.Zero
    var c1 = Offset.Zero
    var end = Offset.Zero

    var index = 0
    var count = points.count()

    path.moveTo(start.x, start.y)

    while ((index + 1) < count) {
      c0 = points[index]
      c1 = points[index + 1]
      end = bezierEndPoint(c0, c1)
      path.quadraticBezierTo(c0.x, c0.y, end.x, end.y)
      index++
    }
    val last = points.last()
    path.quadraticBezierTo(last.x, last.y, start.x, start.y)
    path.close()

    return path
  }

  var path by remember {
    mutableStateOf<Path?>(null)
  }

  AnimatedContent(path) {
    Canvas(
      modifier.fillMaxSize().clip(CircleShape)
    ) {

      val center = Offset(size.width / 2.0f, size.height / 2.0f)
      val radius = size.minDimension / 2.0
      val path0 = getPath(center, radius.toFloat())
      drawPath(path0, color, style = style)
    }
  }
}
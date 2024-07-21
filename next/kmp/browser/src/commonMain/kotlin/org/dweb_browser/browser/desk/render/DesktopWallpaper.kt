package org.dweb_browser.browser.desk.render

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dweb_browser.browser.desk.toIntOffset
import org.dweb_browser.helper.SimplexNoise
import org.dweb_browser.helper.compose.hex
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.rand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun rememberDesktopWallpaper() = remember { DesktopWallpaper() }

class DesktopWallpaper {
  //  var isTapDoAnimation by mutableStateOf(true)
  var circleCount by mutableIntStateOf(3)

  private val circles = mutableStateListOf<DesktopBgCircleModel>().apply {
    addAll(randomCircle(circleCount))
  }

  var hour by mutableStateOf(0).apply {
    val currentMoment: Instant = Clock.System.now()
    val datetimeInSystemZone: LocalDateTime =
      currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    value = datetimeInSystemZone.hour
  }

  fun play() {
    val newCircles = circles.map {
      it.randomUpdate(hour)
    }
    circles.clear()
    circles.addAll(newCircles)
  }

  private val animationDoneFlow = MutableSharedFlow<Unit>()

  @OptIn(FlowPreview::class)
  val onAnimationStopFlow =
    animationDoneFlow.sample(100).shareIn(globalDefaultScope, SharingStarted.Eagerly)

  @Composable
  fun Render(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
      suspend fun observerHourChange(action: (Int) -> Unit) {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInSystemZone = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
        val toggleSeconds = (60 - datetimeInSystemZone.minute) * 60 - datetimeInSystemZone.second
        delay(toggleSeconds.toLong() * 1000)
        action(datetimeInSystemZone.hour)
      }

// 以下为debug代码：用来查看整个壁纸动态效果。
//    var c_hour = 0
//    suspend fun observerHourChange(action: (Int) -> Unit) {
//      delay(5 * 1000)
//      c_hour += 1
//      c_hour %= 24
//      action(c_hour)
//    }

      while (true) {
        observerHourChange { toHour ->
          hour = toHour
          play()
        }
      }
    }

    BoxWithConstraints(
      contentAlignment = Alignment.Center,
      modifier = modifier.fillMaxSize(),
    ) {
      RotatingLinearGradientBox(hour, modifier = Modifier.zIndex(-1f))
      for (circle in circles) {
        circle.Render(this)
      }
    }
  }

  @Composable
  fun RotatingLinearGradientBox(hour: Int, modifier: Modifier) {

    val angle = hour.toFloat() / 24f * 360f + 90f //添加90度的偏移
    val angleRad = angle * PI / 180

    val start = Offset(
      x = 0.5f + 0.5f * cos(angleRad).toFloat(), y = 0.5f - 0.5f * sin(angleRad).toFloat()
    )
    val end = Offset(
      x = 0.5f - 0.5f * cos(angleRad).toFloat(), y = 0.5f + 0.5f * sin(angleRad).toFloat()
    )

    val bgColors = desktopBgPrimaryColorStrings(hour).map {
      Color.hex(it)!!
    }

    AnimatedContent(bgColors, modifier = modifier.fillMaxSize(), transitionSpec = {
      fadeIn().togetherWith(fadeOut())
    }) {
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
  }

  private fun randomCircle(count: Int): List<DesktopBgCircleModel> {
    val offset = {
      Offset(
        random(2f),
        random(2f),
      )
    }

    val radius = {
      (0.1f + Random.nextFloat() * 0.9f) / 4.0f
    }

    val color = {
      val currentMoment: Instant = Clock.System.now()
      val datetimeInSystemZone: LocalDateTime =
        currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
      desktopBgPrimaryRandomColor(datetimeInSystemZone.hour)
    }

    val blur = {
      Random.nextInt(11) * 0.01f + 0.9f
    }

    val list = List(count) {
      DesktopBgCircleModel(
        offset(), radius(), color(), blur(), random(0.4f), random(0.4f)
      )
    }.sortedBy { it.blur }

    return list
  }

  private inner class DesktopBgCircleModel(
    val offset: Offset,
    val radius: Float,
    val color: Color,
    val blur: Float,
    val animationToX: Float = 0f,
    val animationToY: Float = 0f,
    val seed: Int = rand(0, 10000)
  ) {
    val noise = SimplexNoise(seed)

    fun randomUpdate(hour: Int? = null) = DesktopBgCircleModel(
      offset = offset,
      radius = radius,
      color = desktopBgPrimaryRandomColor(hour),
      blur = blur,
      animationToX = random(0.5f),
      animationToY = random(0.5f),
      seed = seed,
    )


    val scaleXAni = Animatable(1f)
    val scaleYValue = Animatable(1f)
    val transformXValue = Animatable(1f)
    val transformYValue = Animatable(1f)
    val colorValue = Animatable(Color.Transparent)

    suspend fun doBubbleAnimation(width: Int, height: Int) = coroutineScope {
      val scaleAnimationSpec = tween<Float>(
        durationMillis = 500, delayMillis = 0, easing = FastOutSlowInEasing
      )

      val transformAnimationSpec = tween<Float>(
        durationMillis = 2000, delayMillis = 0, easing = FastOutSlowInEasing
      )

      launch {
        scaleXAni.animateTo(1.05f, scaleAnimationSpec)
        scaleXAni.animateTo(0.95f, scaleAnimationSpec)
        scaleXAni.animateTo(1.03f, scaleAnimationSpec)
        scaleXAni.animateTo(1.0f, scaleAnimationSpec)
      }

      launch {
        scaleYValue.animateTo(0.95f, scaleAnimationSpec)
        scaleYValue.animateTo(1.05f, scaleAnimationSpec)
        scaleYValue.animateTo(0.97f, scaleAnimationSpec)
        scaleYValue.animateTo(1.00f, scaleAnimationSpec)
      }

      launch {
        transformXValue.animateTo(animationToX * width, transformAnimationSpec)
      }

      launch {
        transformYValue.animateTo(animationToY * height, transformAnimationSpec)
      }

      launch {
        colorValue.animateTo(color, tween(1000, 0, LinearEasing))
      }
    }

    @Composable
    fun Render(box: BoxWithConstraintsScope) {
      val model = this@DesktopBgCircleModel
      val maxWidthPx = box.constraints.maxWidth
      val maxHeightPx = box.constraints.maxHeight
      val maxWidth = box.maxWidth
      val maxHeight = box.maxHeight

      LaunchedEffect(model.offset, model.animationToX, model.animationToY) {
        doBubbleAnimation(maxWidthPx, maxHeightPx)
        animationDoneFlow.emit(Unit)
      }

      val radius = min(maxWidthPx, maxHeightPx) * model.radius
      Box(Modifier.size(radius.dp).offset {
        Offset(
          x = model.offset.x * maxWidthPx / 2,
          y = model.offset.y * maxHeightPx / 2
        ).toIntOffset(1F)
      }.graphicsLayer {
        scaleX = scaleXAni.value
        scaleY = scaleYValue.value
        translationX = transformXValue.value
        translationY = transformYValue.value
      }.background(
        Brush.radialGradient(
          0.0f to colorValue.value, model.blur to colorValue.value, 1.0f to Color.Transparent
        ), CircleShape
      )
      )
    }
  }
}

private fun random(times: Float = 1f) = (Random.nextFloat() - 0.5f) * times

private typealias ColorString = String

internal fun desktopBgPrimaryColorStrings(hour: Int? = null): List<ColorString> {

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


fun desktopBgPrimaryRandomColor(hour: Int? = null): Color {
  val colors = desktopBgPrimaryColorStrings(hour)
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


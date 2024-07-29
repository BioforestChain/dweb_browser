package org.dweb_browser.browser.desk.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dweb_browser.helper.SimplexNoise
import org.dweb_browser.helper.compose.hex
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun rememberDesktopWallpaper(): DesktopWallpaper {
  val scope = rememberCoroutineScope()
  return remember(scope) { DesktopWallpaper(scope) }
}

class DesktopWallpaper(private val scope: CoroutineScope) {
  //  var isTapDoAnimation by mutableStateOf(true)
  var circleCount by mutableIntStateOf(6)

  private val circles = mutableStateListOf<DesktopBgCircleModel>().apply {
    addAll(randomCircle(circleCount))
  }

  private fun randomCircle(count: Int): List<DesktopBgCircleModel> {
    val currentMoment: Instant = Clock.System.now()
    val datetimeInSystemZone: LocalDateTime =
      currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())

    val list = List(count) { index ->
      DesktopBgCircleModel(index)
    }

    return list
  }

  var hour by mutableStateOf(0).apply {
    val currentMoment: Instant = Clock.System.now()
    val datetimeInSystemZone: LocalDateTime =
      currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    value = datetimeInSystemZone.hour
  }

  suspend fun play() = coroutineScope {
    circles.forEach {
      launch {
        it.doBubbleAnimation()
      }
    }
  }

  private var curJob: Job? = null

  fun playJob(): Job {
    curJob?.cancel()
    return scope.launch {
      play()
    }.also { curJob = it }
  }

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


      while (true) {
        observerHourChange { toHour ->
          hour = toHour
          playJob()
        }
      }
    }

    val colors = remember(hour) { desktopBgPrimaryColorStrings(hour) }
    BoxWithConstraints(modifier.fillMaxSize()) {
      // 背景
      RotatingLinearGradientBox(colors, hour, modifier = Modifier.zIndex(0f))
      // 动圈
      for (circle in circles) {
        circle.Render(colors, constraints)
      }
    }
  }

  /**
   * 背景
   */
  @Composable
  fun RotatingLinearGradientBox(colors: List<Color>, hour: Int, modifier: Modifier) {

    val angle = hour.toFloat() / 24f * 360f + 90f //添加90度的偏移
    val angleRad = angle * PI / 180

    val start = Offset(
      x = 0.5f + 0.5f * cos(angleRad).toFloat(), y = 0.5f - 0.5f * sin(angleRad).toFloat()
    )
    val end = Offset(
      x = 0.5f - 0.5f * cos(angleRad).toFloat(), y = 0.5f + 0.5f * sin(angleRad).toFloat()
    )

    AnimatedContent(colors, modifier = modifier.fillMaxSize(), transitionSpec = {
      fadeIn().togetherWith(fadeOut())
    }) {
      Canvas(
        modifier = modifier.fillMaxSize()
      ) {
        drawRect(
          brush = Brush.linearGradient(
            colors = colors,
            start = Offset(size.width * start.x, size.height * start.y),
            end = Offset(size.width * end.x, size.height * end.y)
          )
        )
      }
    }
  }


  private inner class DesktopBgCircleModel(private val seed: Int, initProgress: Float = 0f) {
    val noise = SimplexNoise(seed)

    val progressAni = Animatable(initProgress);

    private val aniSpeed = 1 / 3000f
    val ingSpec = tween<Float>(durationMillis = 3000, easing = LinearEasing)
    val outSpec = tween<Float>(durationMillis = 2000, easing = EaseOutCubic)

    fun doBubbleAnimation() {
      animationEndTime = datetimeNow() + ingSpec.durationMillis
    }

    var animationEndTime by mutableStateOf(0L)

    @Composable
    fun Render(colors: List<Color>, constraints: Constraints) {
      val maxWidthPx = constraints.maxWidth
      val maxHeightPx = constraints.maxHeight
      LaunchedEffect(this, animationEndTime) {
        while (animationEndTime > datetimeNow()) {
          progressAni.animateTo(progressAni.value + aniSpeed * ingSpec.durationMillis, ingSpec)
        }
        val decay = exponentialDecay<Float>(frictionMultiplier = 0.1f, 0.001f)
        progressAni.animateDecay(aniSpeed * 1000, decay)
      }
      val progress = progressAni.value.toDouble()
      val ns = noise.n2d(progress / 30, 2000.0).scale(0.1..0.4)
      val nx = noise.n2d(progress / 10, 200.0).scale(-1.2..1.2)
      val ny = noise.n2d(100.0, progress / 10).scale(-1.2..1.2)
      val nb = noise.n2d(1000.0, progress / 20).scale(0.8..1.2).coerceAtMost(1.0)
      val nc = noise.n2d(progress / 30, 1000.0).scale(0.1..0.4).scale(0..colors.size)
        .coerceAtLeast(0.0) % colors.size
      val colorProgress = nc % 1
      val currColorIndex = nc.toInt()
      val currColor = colors[currColorIndex]
      val nextColor = when (colorProgress) {
        0.0 -> currColor
        else -> colors[(currColorIndex + 1) % colors.size]
      }
      val color = lerp(currColor, nextColor, colorProgress.toFloat())

      val radius = (min(maxWidthPx, maxHeightPx) * ns).dp
      val halfRadiusOffset = -radius / 2
      Box(Modifier.size(radius).offset(halfRadiusOffset, halfRadiusOffset).graphicsLayer {
        translationX = (nx * maxWidthPx).toFloat()
        translationY = (ny * maxHeightPx).toFloat()
      }.run {
        when (nb) {
          1.0 -> background(color, CircleShape)
          else -> background(
            Brush.radialGradient(0f to color, nb.toFloat() to color, 1f to Color.Transparent),
            CircleShape
          )
        }
      })
    }
  }
}

private fun desktopBgPrimaryColorStrings(hour: Int): List<Color> {
  return when (hour) {
    1, 2, 3, 4 -> listOf("#18A0FB", "#1BC47D")
    5, 6 -> listOf("#3a1c71", "#d76d77", "#ffaf7b")
    7, 8, 9 -> listOf("#18A0FB", "#907CFF")
    10, 11, 12, 13 -> listOf("#EE46D3", "#907CFF")
    14, 15, 16 -> listOf("#FFC700", "#EE46D3")
    17, 18, 19, 20, 21, 22 -> listOf("#18A0FB", "#7fffd4")
    23, 0 -> listOf("#315787", "#B8B5D6", "#64ADBD", "#000000")
    else -> listOf("#18A0FB", "#1BC47D")
  }.map { Color.hex(it)!! }
}


//private fun desktopBgPrimaryRandomColor(hour: Int): Color {
//  val colors = desktopBgPrimaryColorStrings(hour)
//  val colorStart = colors.first()
//  val colorEnd = colors.last()
//
//  fun getColor(range: IntRange): Int {
//    val c0 = colorStart.substring(range).toInt(16)
//    val c1 = colorEnd.substring(range).toInt(16)
//    return if (c0 == c1) {
//      255
//    } else if (c0 < c1) {
//      (c0..c1).random()
//    } else {
//      (c1..c0).random()
//    }
//  }
//
//  val color = Color(getColor(1..2), getColor(3..4), getColor(5..6))
//  return color
//}


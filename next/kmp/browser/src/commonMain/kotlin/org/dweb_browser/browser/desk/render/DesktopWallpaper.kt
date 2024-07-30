package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
fun rememberDesktopWallpaper(calculation: DesktopWallpaper.() -> Unit = { }): DesktopWallpaper {
  val scope = rememberCoroutineScope()
  return remember(scope) { DesktopWallpaper(scope).apply(calculation) }
}

class DesktopWallpaper(private val scope: CoroutineScope) {
  //  var isTapDoAnimation by mutableStateOf(true)
  var circleCount by mutableIntStateOf(8)

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

  fun play() {
    circles.forEach {
      it.doBubbleAnimation()
    }
  }

  @Composable
  fun Render(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
      while (true) {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInSystemZone = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
        val toggleSeconds = (60 - datetimeInSystemZone.minute) * 60 - datetimeInSystemZone.second
        delay(toggleSeconds.toLong() * 1000)
        hour = datetimeInSystemZone.hour

        /// 播放动画
        play()
        bgAni.animateTo(hour / 23f)
      }
    }

    val colors = remember(hour) { desktopBgPrimaryColorStrings(hour) }
    val nextColors = remember(hour) { desktopBgPrimaryColorStrings(hour + 1) }
    val circleColors = colors + nextColors
    BoxWithConstraints(modifier.fillMaxSize()) {
      // 预备动画基本参数
      for (circle in circles) {
        circle.PrepareAnimation()
      }
//      // 背景
//      RenderBackground(colors, constraints)
//      // 动圈
//      for (circle in circles) {
//        circle.Render(colors, constraints)
//      }
      Canvas(Modifier.fillMaxSize()) {
        // 背景
        drawBackground(colors)
        // 动圈
        for (circle in circles) {
          circle.drawIntoCanvas(this, circleColors, constraints)
        }
      }
    }
  }

  private val bgAni = Animatable(hour / 23f);


  /**
   * 背景
   */
  private fun DrawScope.drawBackground(colors: List<Color>) {
    val angle = bgAni.value / 24f * 360f + 90f //添加90度的偏移
    val angleRad = angle * PI / 180

    val start = Offset(
      x = 0.5f + 0.5f * cos(angleRad).toFloat(), y = 0.5f - 0.5f * sin(angleRad).toFloat()
    )
    val end = Offset(
      x = 0.5f - 0.5f * cos(angleRad).toFloat(), y = 0.5f + 0.5f * sin(angleRad).toFloat()
    )
    drawRect(
      brush = Brush.linearGradient(
        colors = colors,
        start = Offset(size.width * start.x, size.height * start.y),
        end = Offset(size.width * end.x, size.height * end.y)
      )
    )
  }

  var aniSpeed = 1 / 3000f
  var aniDurationMillis
    get() = ingSpec.durationMillis
    set(value) {
      ingSpec = tween(durationMillis = value, easing = LinearEasing)
    }
  private var ingSpec = tween<Float>(durationMillis = 3000, easing = LinearEasing)

  private inner class DesktopBgCircleModel(private val seed: Int, initProgress: Float = 0f) {
    val noise = SimplexNoise(seed)

    val progressAni = Animatable(initProgress);


    fun doBubbleAnimation() {
      animationEndTime = datetimeNow() + ingSpec.durationMillis
    }

    var animationEndTime by mutableStateOf(0L)

    @Composable
    fun PrepareAnimation() {
      LaunchedEffect(this, animationEndTime) {
        while (animationEndTime > datetimeNow()) {
          progressAni.animateTo(progressAni.value + aniSpeed * ingSpec.durationMillis, ingSpec)
        }
        val decay =
          exponentialDecay<Float>(frictionMultiplier = aniSpeed * aniDurationMillis * 0.1f, 0.001f)
        progressAni.animateDecay(aniSpeed * 1000, decay)
      }
    }


    fun drawIntoCanvas(
      scope: DrawScope,
      colors: List<Color>,
      constraints: Constraints,
    ) {
      val maxWidthPx = constraints.maxWidth
      val maxHeightPx = constraints.maxHeight
      val progress = progressAni.value.toDouble()
      val n_size = noise.n2d(progress / 30, 2000.0).scale(0.1..0.4).toFloat()
      val n_x = noise.n2d(progress / 10, 200.0).scale(-1.2..1.2).toFloat()
      val n_y = noise.n2d(100.0, progress / 10).scale(-1.2..1.2).toFloat()
      val n_brush = noise.n2d(1000.0, progress / 20).scale(0.8..1.2).toFloat()
      val n_color = noise.n2d(progress / 30, 1000.0).scale(0.1..0.4).scale(colors.indices).toFloat()
        .coerceAtLeast(0.0f) % colors.size
      val n_alpha = noise.n2d(progress / 20, 2000.0).toFloat()
      val colorProgress = n_color % 1
      val currColorIndex = n_color.toInt()
      val currColor = colors[currColorIndex].copy(alpha = n_alpha)
      val nextColor = when (colorProgress) {
        0.0f -> currColor
        else -> colors[(currColorIndex + 1) % colors.size]
      }.copy(alpha = n_alpha)
      val color = lerp(currColor, nextColor, colorProgress)

      val radius = (min(maxWidthPx, maxHeightPx) * n_size)
      val halfRadiusOffset = -radius / 2

      scope.withTransform({
        translate(
          left = halfRadiusOffset + (n_x * maxWidthPx),
          top = halfRadiusOffset + (n_y * maxHeightPx),
        )
      }) {
        val blendMode = when {
          seed % 2 == 0 -> BlendMode.Overlay
          else -> BlendMode.Lighten
        }
        when {
          n_brush >= 1f -> drawCircle(
            brush = Brush.verticalGradient(
              0f to nextColor,
              (n_brush - 1f) / 0.2f to color,
              1f to color,
              startY = 0f,
              endY = radius * 2
            ),
            radius = radius,
            blendMode = blendMode,
          )

          else -> drawCircle(
            brush = Brush.radialGradient(
              0f to color,
              n_brush to color,
              1f to Color.Transparent
            ),
            radius = radius,
            blendMode = blendMode,
          )
        }
      }
    }
  }
}

private fun desktopBgPrimaryColorStrings(hour: Int): List<Color> {
  return when (hour % 24) {
    0 -> listOf("#9c27b0", "#e91e63")
    1 -> listOf("#3973e1", "#c03074")
    2 -> listOf("#6b4dc8", "#d4276c")
    3 -> listOf("#0899f9", "#ab397d")
    4 -> listOf("#add07a", "#6349a2")
    5 -> listOf("#5ab4ba", "#874190")
    6 -> listOf("#ffeb3b", "#3f51b5")
    7 -> listOf("#55cca1", "#178cdf")
    8 -> listOf("#aadb6e", "#2b6eca")
    9 -> listOf("#00bcd4", "#03a9f4")
    10 -> listOf("#aadb6e", "#179cf3")
    11 -> listOf("#55cca1", "#0da3f4")
    12 -> listOf("#ffeb3b", "#2196f3")
    13 -> listOf("#f6a940", "#38b9e4")
    14 -> listOf("#faca3d", "#2da7eb")
    15 -> listOf("#f18842", "#44cadc")
    16 -> listOf("#fa672d", "#c1a949")
    17 -> listOf("#f67837", "#82b993")
    18 -> listOf("#ff5722", "#ff9800")
    19 -> listOf("#ff7811", "#ffc21e")
    20 -> listOf("#ff9800", "#ffeb3b")
    21 -> listOf("#b54384", "#ef5159")
    22 -> listOf("#ce6058", "#f4854f")
    23 -> listOf("#e67c2c", "#fab845")
    else -> listOf("#ffeb3b", "#2196f3")
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


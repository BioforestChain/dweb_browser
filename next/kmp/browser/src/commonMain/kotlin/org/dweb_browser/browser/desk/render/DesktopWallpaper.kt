package org.dweb_browser.browser.desk.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
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

/// 测试代码 记住当前时间的小时数
//    var hour by remember { mutableStateOf(0) }
//    // 使用 LaunchedEffect 实现每两秒钟更换一次配色
//    LaunchedEffect(Unit) {
//      while (true) {
//        delay(2000) // 等待两秒钟
//        hour = (hour + 1) % 24 // 更新小时数，保持在 0 到 23 之间循环
//      }
//    }
/// 测试代码结束

    // 根据小时数获取颜色列表
    val colors = remember(hour) { desktopBgPrimaryColorStrings(hour) }
//    val nextColors = remember(hour) { desktopBgPrimaryColorStrings(hour + 1) }
//    val circleColors = colors + nextColors
    BoxWithConstraints(modifier.fillMaxSize()) {
      // 预备动画基本参数
//      for (circle in circles) {
//        circle.PrepareAnimation()
//      }
      Canvas(Modifier.fillMaxSize()) {
        // 背景
        drawBackground(colors)
        // 动圈
//        for (circle in circles) {
//          circle.drawIntoCanvas(this, circleColors, constraints)
//        }
      }
// 测试代码
//      Text("$hour", modifier.offset(26.dp, 26.dp), fontWeight = FontWeight.Bold)
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

/**
 * 根据时间输出背景颜色
 */
private fun desktopBgPrimaryColorStrings(hour: Int): List<Color> {
  return when (hour % 24) {
    0 -> listOf("#b565c8", "#7b97c8")
    1 -> listOf("#6b97e8", "#1157b1")
    2 -> listOf("#8f7ad5", "#de6a97")
    3 -> listOf("#4eb3ba", "#bf6da0")
    4 -> listOf("#bfda9c", "#8875b7")
    5 -> listOf("#86c7cc", "#a46daa")
    6 -> listOf("#ffb06b", "#7283cb")
    7 -> listOf("#fbc15f", "#5caee7")
    8 -> listOf("#bde391", "#6494d7")
    9 -> listOf("#6494d7", "#bde3a2")
    10 -> listOf("#92b1ff", "#2fc6f2")
    11 -> listOf("#62b1cf", "#a2cba9")
    12 -> listOf("#6dbbe1", "#bde3a2")
    13 -> listOf("#84d7b9", "#6dcbeb")
    14 -> listOf("#65bfef", "#fbd7a0")
    15 -> listOf("#f5ca81", "#77d7e5")
    16 -> listOf("#c1d7ca", "#fb8d5f")
    17 -> listOf("#fb8d5f", "#d1badf")
    18 -> listOf("#faa780", "#c1a949")
    19 -> listOf("#f37e84", "#6b97e8")
    20 -> listOf("#da8780", "#d1bf7a")
    21 -> listOf("#8f71a5", "#f37e84")
    22 -> listOf("#a47fa6", "#ecbb51")
    23 -> listOf("#ec9b5f", "#a46daa")
    else -> listOf("#84d7b9", "#62b1f6")
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


package org.dweb_browser.browser.desk

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.hex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun desktopWallpaperView(circleCount: Int, modifier: Modifier, onClick: (()->Unit)? = null) {

  val circles = remember {
    mutableStateListOf<DesktopBgCircleModel>().apply {
      addAll(DesktopBgCircleModel.randomCircle(circleCount))
    }
  }

  var hour by remember {
    val currentMoment: Instant = Clock.System.now()
    val datetimeInSystemZone: LocalDateTime =
      currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
    mutableStateOf<Int>(datetimeInSystemZone.hour)
  }

  fun updateCircle() {
    val newCircles = circles.map {
      DesktopBgCircleModel.randomUpdate(it, hour)
    }
    circles.clear()
    circles.addAll(newCircles)
  }

  LaunchedEffect(Unit) {
    var c_hour = 0
    suspend fun observerHourChange(action: (Int) -> Unit) {
      val currentMoment: Instant = Clock.System.now()
      val datetimeInSystemZone = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
//      val triggleSeconds = (60 - datetimeInSystemZone.minute) * 60 - datetimeInSystemZone.second
      val triggleSeconds = 5 - datetimeInSystemZone.second % 5
      delay(triggleSeconds.toLong() * 1000)
//      c_hour += 1
//      c_hour %= 24
//      action(c_hour)
      action(datetimeInSystemZone.hour)
    }

    while (true) {
      observerHourChange { toHour ->
        hour = toHour
        updateCircle()
      }
    }
  }

  BoxWithConstraints(
    modifier
      .fillMaxSize()
      .clickableWithNoEffect {
        println("Mike tap")
        onClick?.let {
          println("Mike tap tap tap")
          updateCircle()
          onClick()
        }
      }
  ) {
    RotatingLinearGradientBox(hour, modifier = Modifier.zIndex(-1f))
    circles.forEach {
      DesktopBgCircle(it)
    }
  }
}

@Composable
fun RotatingLinearGradientBox(hour: Int, modifier: Modifier) {
  // 当前时间的角度 + 90f， 默认是从270度开始的，所以添加90度的偏移
  val angle = hour.toFloat() / 24f * 360f + 90f
  // 将角度转换为弧度, 逆时针旋转
  val angleRad = angle * PI / 180
  // 计算起点和终点
  val start = Offset(
    x = 0.5f + 0.5f * cos(angleRad).toFloat(),
    y = 0.5f - 0.5f * sin(angleRad).toFloat()
  )
  val end = Offset(
    x = 0.5f - 0.5f * cos(angleRad).toFloat(),
    y = 0.5f + 0.5f * sin(angleRad).toFloat()
  )

  val bgColors = desktopBgPrimaryColorStrings(hour).map {
    Color.hex(it)!!
  }

  AnimatedContent(bgColors,
    modifier = modifier.fillMaxSize(),
    transitionSpec = {
      fadeIn().togetherWith(fadeOut())
    }
  ) {
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


@Composable
fun BoxWithConstraintsScope.DesktopBgCircle(
  model: DesktopBgCircleModel
) {
  val scope = rememberCoroutineScope()
  val scaleXValue = remember { Animatable(1f) }
  val scaleYValue = remember { Animatable(1f) }
  val transformXValue = remember { Animatable(1f) }
  val transformYValue = remember { Animatable(1f) }
  val colorValue = remember { Animatable(Color.Transparent) }

  fun doBubbleAnimation() {
    scope.launch {
      val scaleAnimationSpec = tween<Float>(
        durationMillis = 500,
        delayMillis = 0,
        easing = FastOutSlowInEasing
      )

      val transformAnimationSpec = tween<Float>(
        durationMillis = 2000,
        delayMillis = 0,
        easing = FastOutSlowInEasing
      )

      launch {
        scaleXValue.animateTo(1.05f, scaleAnimationSpec)
        scaleXValue.animateTo(0.95f, scaleAnimationSpec)
        scaleXValue.animateTo(0.97f, scaleAnimationSpec)
        scaleXValue.animateTo(1.0f, scaleAnimationSpec)
      }

      launch {
        scaleYValue.animateTo(0.95f, scaleAnimationSpec)
        scaleYValue.animateTo(1.05f, scaleAnimationSpec)
        scaleYValue.animateTo(0.97f, scaleAnimationSpec)
        scaleYValue.animateTo(1.00f, scaleAnimationSpec)
      }

      launch {
        transformXValue.animateTo(model.animationToX * maxWidth.value, transformAnimationSpec)
      }

      launch {
        transformYValue.animateTo(model.animationToY * maxWidth.value, transformAnimationSpec)
      }

      launch {
        colorValue.animateTo(model.color, tween(3000, 0, LinearEasing))
      }
    }
  }

  LaunchedEffect(model.offset, model.animationToX, model.animationToY) {
    doBubbleAnimation()
  }
  val width = constraints.maxWidth
  val height = constraints.maxHeight
  Box(modifier = Modifier
    .offset {
      Offset(
        x = model.offset.x * width / 2,
        y = model.offset.y * height / 2
      )
        .toIntOffset(1F)
    }
    .graphicsLayer {
      scaleX = scaleXValue.value
      scaleY = scaleYValue.value
      translationX = transformXValue.value
      translationY = transformYValue.value
    }
    .blur(model.blur.dp)
  )
  {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = colorValue.value,
        model.radius * width / 4f,
      )
    }
  }

}


private fun random(times: Float = 1f) = (Random.nextFloat() - 0.5f) * times

/*
        // 晚上
        19, 20, 21, 22: multiply #00C5DF #00C5DF #00C5D #F2371F
        // 极光
        23, 0, 1: overlay #315787 #B8B5D6 #64ADBD #6B93C6 #000000
        // 凌晨
        1, 2, 3, 4: multiply #18A0FB #1BC47D
        // 日出
        5, 6: luminosity #18A0FB #1BC47D #18A0FB #FFC700 #FFC700 #F2371F
        // 早上
        7,8,9,: overlay #18A0FB #907CFF
        // 中午
        10,11,12,13: overlay #EE46D3 #907CFF
        // 下午
        14, 15, 16: overlay #FFC700 #EE46D3
        // 日落
        17,18: overlay #F2371F #18A0FB #907CFF #FFC700
* */

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

internal fun desktopBgPrimaryColors(our: Int? = null) = desktopBgPrimaryColorStrings(null).map {
  Color.hex(it)!!
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


data class DesktopBgCircleModel(
  var offset: Offset,
  var radius: Float,
  var color: Color,
  var blur: Int,
  var animationToX: Float = 0f,
  var animationToY: Float = 0f,
) {
  companion object {

    fun randomUpdate(origin: DesktopBgCircleModel, hour: Int? = null) = origin.copy(color = desktopBgPrimaryRandomColor(hour),
      animationToX = random(0.5f),
      animationToY = random(0.5f)
    )

    fun randomCircle(count: Int): List<DesktopBgCircleModel> {
      val list = mutableListOf<DesktopBgCircleModel>()

      val offset = {
        Offset(
          Random.nextFloat() * 2f - 1f,
          Random.nextFloat() * 2f - 1f,
        )
      }

      val radius = {
        Random.nextFloat()
      }

      val color = {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInSystemZone: LocalDateTime =
          currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
        desktopBgPrimaryRandomColor(datetimeInSystemZone.hour)
      }

      val blur = {
        Random.nextInt(5) + 1
      }

      (1..count).forEach {
        list.add(DesktopBgCircleModel(offset(), radius(), color(), blur(), random(0.4f), random(0.4f)))
      }

      list.sortBy {
        it.blur
      }

      return list.reversed()
    }
  }
}
package org.dweb_browser.browser.desk.render

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.render.activity.ActivityItemContentRender
import org.dweb_browser.helper.compose.clickableWithNoEffect
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape
import kotlin.math.max

@Composable
actual fun ActivityController.Render() {
  val density = LocalDensity.current
  val paddingTop = max(WindowInsets.safeGestures.getTop(density) / density.density, 32f)
  Box(
    Modifier
      .fillMaxSize()
      .padding(top = paddingTop.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Button(onClick = {
      request(
        deskNMM,
        icon = ActivityItem.NoneIcon,
        content = ActivityItem.TextContent("Hello", "Hello Gaubee, This is Long Text!!!"),
        action = ActivityItem.NoneAction,
      )
    }, modifier = Modifier.align(Alignment.BottomCenter)) {
      Text(text = "创建活动")
    }
    val activityList by list.collectAsState()

    val showList = mutableListOf<ActivityItem>()
    for (index in activityList.indices.reversed()) {
      val activity = activityList[index]
      if (!activity.renderProp.open && activity.renderProp.viewAni.value == 0f) {
        break
      }
      showList.add(activity)
    }
    showList.reversed().forEach { activity ->
      key(activity.id) {
        ActivityItemRender(this@Render, activity, paddingTop)
      }
    }
  }
}

private fun <T> enterAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioLowBouncy,
  stiffness = Spring.StiffnessMedium / 100,
)

private fun <T> exitAnimationSpec() = spring<T>(
  dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = Spring.StiffnessLow / 100,
)

@SuppressLint("RestrictedApi")
@Composable
private fun ActivityItemRender(
  controller: ActivityController,
  item: ActivityItem,
  paddingTop: Float,
) {
  val renderProp = item.renderProp
  val p1 = renderProp.viewAni.value

  LaunchedEffect(renderProp.open) {
    if (renderProp.open) {
      renderProp.viewAni.animateTo(1f, enterAnimationSpec())
    } else {
      renderProp.viewAni.animateTo(0f, exitAnimationSpec())
    }
  }

  if (p1 == 0f) {
    return
  }

  val toastModifier: Modifier = when {
    renderProp.viewAni.isRunning -> {
      val blur = ((1f - p1) * 16f).coerceAtLeast(0f)
      Modifier
        .graphicsLayer {
          translationY = (1 - p1) * -paddingTop * density
          alpha = when {
            renderProp.open -> 0.5f + 0.5f * p1
            else -> 0.2f + 0.8f * p1
          }
          val scale = when {
            renderProp.open -> 0.5f + 0.5f * p1
            else -> 0.2f + 0.8f * p1
          }
          scaleX = scale
          scaleY = scale
        }
        .blur(blur.dp)
        .padding(blur.coerceAtLeast(0f).dp)
    }

    else -> Modifier
  }

  val elevation = (16 * p1).dp
  val contentPadding = 16f
  val innerPaddingDp = (contentPadding / 4).dp
  val shape = SquircleShape(
    (contentPadding + renderProp.detailAni.value * contentPadding).dp,
    CornerSmoothing.Medium
  )

  Box(
    toastModifier.background(Color.Black, shape = shape),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .animateContentSize()
        .then(
          when {
            p1 < 0.5f -> {
              Modifier
                .size(0.dp)
                .alpha(0f)
            }

            p1 == 1f -> Modifier.wrapContentSize()

            else -> Modifier
              .wrapContentSize()
              .alpha((p1 - 0.5f) * 2)
          }
        )
        .padding(innerPaddingDp)
        .composed {
          var dragMove by remember { mutableFloatStateOf(0f) }
          pointerInput(Unit) {
            detectVerticalDragGestures(onDragEnd = {
              println("QAQ dragMove=$dragMove")
              renderProp.open = dragMove > 0
              if (renderProp.open) {
                renderProp.showDetail = true
              }
            }) { change, dragAmount ->
              change.consume()
              dragMove += dragAmount
            }
          }
        }
        .clickableWithNoEffect {
          renderProp.showDetail = !renderProp.showDetail
        },
      contentAlignment = Alignment.Center,
    ) {
      ActivityItemContentRender(
        controller,
        item,
        innerPaddingDp
      )
    }
  }
}

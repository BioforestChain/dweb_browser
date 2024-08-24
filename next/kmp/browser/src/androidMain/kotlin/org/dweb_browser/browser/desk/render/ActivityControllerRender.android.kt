package org.dweb_browser.browser.desk.render

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.render.activity.ActivityItemContentRender
import org.dweb_browser.browser.desk.render.activity.activityEnterAnimationSpec
import org.dweb_browser.browser.desk.render.activity.activityExitAnimationSpec
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
        ActivityItem(
          owner = deskNMM,
          leadingIcon = ActivityItem.NoneIcon,
          trailingIcon = ActivityItem.ImageIcon("http://192.168.2.14:14433/animated-webp-supported.webp"),
          centerTitle = ActivityItem.TextContent("Hello Gaubee, This is Long Text!!!"),
          bottomActions = emptyList(),
        )
      )
    }, modifier = Modifier.align(Alignment.BottomCenter)) {
      Text(text = "创建活动")
    }
    val activityList by list.collectAsState()
    val showList = mutableListOf<ActivityItem>()
    for (index in activityList.indices.reversed()) {
      val activity = activityList[index]
      if (!activity.renderProp.canView) {
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
      renderProp.viewAni.animateTo(1f, activityEnterAnimationSpec())
    } else {
      renderProp.viewAni.animateTo(0f, activityExitAnimationSpec())
    }
  }
  if (!renderProp.canView) {
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
  val contentPadding = 16f
  val innerPadding1 = contentPadding / 2
  val innerPadding2 = innerPadding1 / 2
  val innerPaddingDp = lerp(innerPadding2, innerPadding1, renderProp.detailAni.value).dp
  val elevation = lerp(0f, contentPadding, renderProp.detailAni.value)
  val shape = SquircleShape(
    lerp(contentPadding, contentPadding * 2, renderProp.detailAni.value).dp,
    CornerSmoothing.Medium
  )

  Box(
    toastModifier
      .shadow(
        elevation.dp, shape = shape,
      )
      .background(Color.Black, shape = shape),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .wrapContentSize()
        .let { modifier ->
          when {
            !renderProp.viewAni.isRunning -> modifier
            p1 < 0.5f -> {
              modifier
                .animateContentSize()
                .size(0.dp)
                .alpha(0f)
            }

            else -> modifier
              .animateContentSize()
              .alpha((p1 - 0.5f) * 2)
          }
        }
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

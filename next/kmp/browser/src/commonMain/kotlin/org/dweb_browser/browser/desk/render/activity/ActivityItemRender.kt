package org.dweb_browser.browser.desk.render.activity

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.browser.desk.model.ActivityStyle
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.saveBlur
import org.dweb_browser.sys.window.render.AppLogo
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

@Composable
internal fun ActivityItemRender(
  controller: ActivityController,
  item: ActivityItem,
  style: ActivityStyle,
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
  /**
   * viewAni == 0 -> openScreenMarginTop
   * detailAni == 1 -> openScreenMarginTop
   *
   * viewAni == 1 -> screenMarginTop
   * detailAni == 0 -> screenMarginTop
   */
  val offset = lerp(style.screenMarginTop, style.openScreenMarginTop, renderProp.detailAni.value)
  style.containerBox(
    ActivityStyle.ContainerScope(
      lerp(
        style.openScreenMarginTop,
        offset,
        renderProp.viewAni.value
      ).dp
    )
  ) {
    val toastModifier: Modifier = when {
      renderProp.viewAniFinished -> Modifier
      else -> {
        val blur = lerp(16f, 0f, renderProp.viewAni.value).coerceAtLeast(0f)
        Modifier
          .graphicsLayer {
            translationY = (1 - p1) * -style.screenMarginTop * density
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
          .saveBlur(blur.dp)
          .padding(blur.coerceAtLeast(0f).dp)
      }
    }
    val contentPadding = 16f
    val innerPadding1 = contentPadding / 2
    val innerPadding2 = innerPadding1 / 2
    val innerPaddingDp = lerp(innerPadding2, innerPadding1, renderProp.detailAni.value).dp
    val elevation =
      lerp(style.shadowElevation, style.openShadowElevation, renderProp.detailAni.value)
    val shape = SquircleShape(
      lerp(style.radius, style.openRadius, renderProp.detailAni.value).dp,
      CornerSmoothing.Medium
    )

    style.contentBox(
      ActivityStyle.ContentScope(
        toastModifier
          .shadow(elevation.dp, shape = shape)
          .background(Color.Black, shape = shape)
      )
    ) {
      Box(
        Modifier
          .wrapContentSize()
          .let { modifier ->
            when {
              renderProp.viewAniFinished -> modifier
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
        ActivityItemLayout(
          controller = controller,
          item = item,
          style = style,
          innerPaddingDp = innerPaddingDp,
        )
      }
    }
  }
}

@Composable
internal fun ActivityItemLayout(
  controller: ActivityController,
  item: ActivityItem,
  style: ActivityStyle,
  innerPaddingDp: Dp,
) {
  val renderProp = item.renderProp
  ActivityItemContentEffect(renderProp)
  CompositionLocalProvider(
    LocalContentColor provides Color.White,
    LocalTextStyle provides MaterialTheme.typography.bodySmall,
  ) {
    val centerWidthDp = lerp(
      style.centerWidth,
      style.openCenterWidth,
      renderProp.detailAni.value
    ).dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        Modifier.height(
          lerp(
            style.overlayCutoutHeight,
            style.openOverlayCutoutHeight,
            renderProp.detailAni.value
          ).coerceAtLeast(0f).dp
        )
      )
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val minSize = lerp(24f, 48f, renderProp.detailAni.value).dp
        Box(
          modifier = Modifier
            .padding(innerPaddingDp)
            .requiredSize(minSize)
        ) {
          AppLogo.from(
            resource = item.appIcon,
            fetchHook = controller.deskNMM.blobFetchHook,
          ).Render(Modifier.fillMaxWidth())
          val marginDp = lerp(24f * 0.13f, 0f, renderProp.detailAni.value).coerceAtLeast(0f).dp
          item.leadingIcon.Render(
            controller,
            renderProp,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .requiredSize(lerp(0f, 24f, renderProp.detailAni.value).dp)
              .padding(end = marginDp, bottom = marginDp)
          )
        }
        Box(modifier = Modifier.width(centerWidthDp))

        Box(
          modifier = Modifier
            .padding(innerPaddingDp)
            .requiredSize(minSize)
        ) {
          item.trailingIcon.Render(controller, renderProp, Modifier.fillMaxSize())
        }
      }
      val aniHeight = lerp(0f, innerPaddingDp.value * 2 + 24, renderProp.detailAni.value)
      if (aniHeight > 0f) {
        Box(Modifier.requiredHeight(aniHeight.dp)) {
          item.centerTitle.Render(
            renderProp,
            modifier = Modifier
              .padding(innerPaddingDp)
              .requiredSize(centerWidthDp, 24.dp)
              .graphicsLayer { scaleY = renderProp.detailAni.value },
          )
        }
      }
      if (item.bottomActions.isNotEmpty() && renderProp.canViewDetail) {
        Row {
          for (action in item.bottomActions) {
            key(action) {
              action.Render(controller, renderProp)
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun ActivityItemContentEffect(renderProp: ActivityItemRenderProp) {
  LaunchedEffect(renderProp.showDetail) {
    if (renderProp.showDetail) {
      renderProp.detailAni.animateTo(1f, activityEnterAnimationSpec())
      delay(5000)
      renderProp.showDetail = false
    } else {
      renderProp.detailAni.animateTo(0f, activityExitAnimationSpec())
    }
  }
}
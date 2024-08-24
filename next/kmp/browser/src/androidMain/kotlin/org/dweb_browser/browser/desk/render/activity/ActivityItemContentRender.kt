package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.sys.window.render.AppLogo

@Composable
internal fun ActivityItemContentRender(
  controller: ActivityController,
  item: ActivityItem,
  innerPaddingDp: Dp,
) {
  val renderProp = item.renderProp
  ActivityItemContentEffect(renderProp)
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        item.leadingIcon.Render(
          controller,
          renderProp,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .requiredSize(lerp(0f, 24f, renderProp.detailAni.value).dp)
        )
      }
      Box(modifier = Modifier.width(item.centerWidth.dp))

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
            .requiredSize(item.centerWidth.dp, 24.dp)
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
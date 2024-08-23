package org.dweb_browser.browser.desk.render.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.browser.web.ui.enterAnimationSpec
import org.dweb_browser.browser.web.ui.exitAnimationSpec
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.render.AppIconContainer
import org.dweb_browser.sys.window.render.AppLogo


@Composable
internal fun ActivityItemContentRender(
  controller: ActivityController,
  item: ActivityItem,
  innerPaddingDp: Dp,
) {
  val renderProp = item.renderProp
  ActivityItemContentEffect(renderProp)
  @OptIn(ExperimentalSharedTransitionApi::class) SharedTransitionScope { modifier ->
    LazyColumn(modifier) {
      item("header") {
        Row(
          modifier = Modifier
            .animateContentSize(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AnimatedVisibility(renderProp.showDetail, label = "header-app-logo") {
            AppLogo.from(
              resource = item.appIcon,
              fetchHook = controller.deskNMM.blobFetchHook,
            ).Render(
              Modifier
                .padding(innerPaddingDp)
                .size(48.dp)
                .sharedElement(rememberSharedContentState(key = "app-logo"), this)
                .sharedBounds(rememberSharedContentState(key = "app-logo:bounds"), this)
            )
          }
          if (renderProp.showDetail) {
            when (val action = item.action) {
              ActivityItem.NoneAction -> {}
              is ActivityItem.CancelAction -> FilledTonalButton(
                onClick = {
                  action.uri?.also { uri ->
                    controller.deskNMM.scopeLaunch(cancelable = true) {
                      controller.deskNMM.nativeFetch(uri)
                    }
                  }
                  renderProp.open = false
                }, colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.error,
                )
              ) {
                Text(action.text)
              }

              is ActivityItem.ConfirmAction -> FilledTonalButton(
                onClick = {
                  action.uri?.also { uri ->
                    controller.deskNMM.scopeLaunch(cancelable = true) {
                      controller.deskNMM.nativeFetch(uri)
                    }
                  }
                  renderProp.open = false
                }, colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.primary,
                )
              ) {
                Text(action.text)
              }

              is ActivityItem.LinkAction -> TextButton(onClick = {
                controller.deskNMM.scopeLaunch(cancelable = true) {
                  controller.deskNMM.nativeFetch(action.uri)
                }
                renderProp.open = false
              }) {
                Text(action.text)
              }
            }
          }
        }
      }
      item("body") {
        LazyRow(
          modifier = Modifier
            .animateContentSize(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          item("icon") {

            val iconSize = when {
              renderProp.showDetail -> 48.dp
              else -> 24.dp
            }
            when (val icon = item.icon) {
              is ActivityItem.ImageIcon -> PureImageLoader.SmartLoad(
                url = icon.url,
                maxWidth = iconSize,
                maxHeight = iconSize,
                currentColor = null,
                hook = controller.deskNMM.blobFetchHook,
              ).with {
                Box(
                  modifier = Modifier
                    .size(iconSize)
                    .clip(AppIconContainer.defaultShape)
                    .background(Color.White)
                ) {
                  Image(it, "activity-icon", contentScale = ContentScale.Crop)
                }
              }

              ActivityItem.NoneIcon -> Box(
                Modifier
                  .padding(innerPaddingDp)
                  .size(iconSize)
              ) {
                AnimatedVisibility(
                  !renderProp.showDetail,
                  label = "body-app-logo"
                ) {
                  AppLogo.from(
                    resource = item.appIcon,
                    fetchHook = controller.deskNMM.blobFetchHook,
                  ).Render(
                    Modifier
                      .fillMaxSize()
                      .sharedElement(rememberSharedContentState(key = "app-logo"), this)
                      .sharedBounds(rememberSharedContentState(key = "app-logo:bounds"), this)
                  )
                }
              }
            }
          }

          item("content") {

            when (val content = item.content) {
              is ActivityItem.TextContent -> {
                ActivityTextContent(renderProp, content, innerPaddingDp, Modifier.animateItem())
              }
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
      renderProp.detailAni.animateTo(1f, enterAnimationSpec())
      delay(5000)
      renderProp.showDetail = false
    } else {
      renderProp.detailAni.animateTo(0f, exitAnimationSpec())
    }
  }
}
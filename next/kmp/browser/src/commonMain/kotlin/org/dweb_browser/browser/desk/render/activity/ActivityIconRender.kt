package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.dweb_browser.browser.desk.ActivityController
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.pure.image.compose.LocalCoilImageLoader
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad

@Composable
fun ActivityItem.Icon.Render(
  controller: ActivityController,
  renderProp: ActivityItemRenderProp,
  modifier: Modifier = Modifier,
) {
  when (val icon = this) {
    is ActivityItem.ImageIcon -> icon.Render(controller, renderProp, modifier)
    is ActivityItem.ComposeIcon -> icon.Render(controller, renderProp, modifier)
    ActivityItem.NoneIcon -> {}
  }
}

@Composable
fun ActivityItem.ImageIcon.Render(
  controller: ActivityController,
  renderProp: ActivityItemRenderProp,
  modifier: Modifier = Modifier,
) {
  PureImageLoader.SmartLoad(
    url = url,
    maxWidth = 128.dp,
    maxHeight = 128.dp,
    currentColor = null,
//    hook = controller.deskNMM.blobFetchHook,
  ).with {
    coilImageRequest?.also { imageRequest ->
      val painter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = LocalCoilImageLoader.current.ImageLoader()
      )
      Image(painter = painter, contentDescription = null, modifier = modifier)
    } ?: Image(bitmap = it, contentDescription = null, modifier = modifier)
  }
}

@Composable
fun ActivityItem.ComposeIcon.Render(
  controller: ActivityController,
  renderProp: ActivityItemRenderProp,
  modifier: Modifier = Modifier,
) {
  content(modifier)
}
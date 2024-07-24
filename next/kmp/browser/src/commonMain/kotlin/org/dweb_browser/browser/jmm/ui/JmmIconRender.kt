package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BrokenImage
import androidx.compose.material.icons.twotone.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

@Composable
fun JmmAppInstallManifest.IconRender(size: Dp = 36.dp) {
  key(logo) {
    val modifier = remember(size) { Modifier.size(size).clip(RoundedCornerShape(size / 5)) }
    PureImageLoader.SmartLoad(logo, size, size, LocalWindowMM.current.blobFetchHook)
      .with(onBusy = { reason ->
        Image(
          imageVector = Icons.TwoTone.Image,
          contentDescription = reason,
          modifier = modifier,
          alpha = 0.5f
        )
      }, onError = {
        Image(
          imageVector = Icons.TwoTone.BrokenImage,
          contentDescription = "icon load error: ${it.message}",
          modifier = modifier,
        )
      }) {
        Image(
          bitmap = it,
          contentDescription = "${short_name}'s icon",
          modifier = modifier,
        )
      }
  }
}
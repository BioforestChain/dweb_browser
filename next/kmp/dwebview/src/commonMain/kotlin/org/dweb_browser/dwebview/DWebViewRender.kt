package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.compose.UnScaleBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isIOS

@Composable
expect fun IDWebView.Render(
  modifier: Modifier = Modifier,
  onCreate: (suspend IDWebView.() -> Unit)? = null,
  onDispose: (suspend IDWebView.() -> Unit)? = null,
)

@Composable
fun IDWebView.RenderWithScale(
  scale: Float,
  modifier: Modifier = Modifier,
  onCreate: (suspend IDWebView.() -> Unit)? = null,
  onDispose: (suspend IDWebView.() -> Unit)? = null,
) {
  if (IPureViewController.isIOS) {
    UnScaleBox(scale, modifier) {
      ScaleEffect(scale, Modifier.matchParentSize())
      Render(Modifier.matchParentSize(), onCreate, onDispose)
    }
  } else {
    UnScaleBox(scale,modifier) {
      ScaleEffect(scale, Modifier.matchParentSize())
      Render(Modifier.matchParentSize(), onCreate, onDispose)
    }
  }
}

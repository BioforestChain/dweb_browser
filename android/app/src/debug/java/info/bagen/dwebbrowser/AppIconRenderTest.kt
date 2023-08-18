package info.bagen.dwebbrowser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.dweb_browser.window.render.AppIconRender

@Composable
fun PreviewAppIconRender(resource: Painter, color: Color = Color.Magenta) {
  Column {
    Row(modifier = Modifier.weight(1f)) {
      AppIconRender(
        modifier = Modifier.weight(1f),
        color = color,
        iconPlaceholder = resource,
        alwaysSuccess = true,
      )
      AppIconRender(
        modifier = Modifier.weight(1f),
        color = color,
        iconPlaceholder = resource,
        iconMaskable = true,
        alwaysSuccess = true,
      )
    }
    Row(modifier = Modifier.weight(1f)) {
      AppIconRender(
        modifier = Modifier.weight(1f),
        color = color,
        iconPlaceholder = resource,
        iconMonochrome = true,
        alwaysSuccess = true,
      )
      AppIconRender(
        modifier = Modifier.weight(1f),
        color = color,
        iconPlaceholder = resource,
        iconMaskable = true,
        iconMonochrome = true,
        alwaysSuccess = true,
      )
    }
  }
}

@Preview(widthDp = 120, heightDp = 120)
@Composable
fun PreviewAppIconRenderByImage(modifier: Modifier = Modifier) {
  PreviewAppIconRender(
    painterResource(
      id = R.drawable.m3_favicon_apple_touch
    )
  )
}


@Preview(widthDp = 120, heightDp = 120)
@Composable
fun PreviewAppIconRenderByVector(modifier: Modifier = Modifier) {
  PreviewAppIconRender(
    painterResource(
      id = R.drawable.blank_app
    )
  )
}


@Preview(widthDp = 120, heightDp = 120)
@Composable
fun PreviewAppIconRenderByVector2(modifier: Modifier = Modifier) {
  PreviewAppIconRender(
    painterResource(
      id = R.drawable.blank_app
    ),
    color = Color.Cyan
  )
}


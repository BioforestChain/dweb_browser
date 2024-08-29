package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalGraphicsContext

@Composable
fun rememberMultiGraphicsLayers(size: Int): List<GraphicsLayer> {
  val graphicsContext = LocalGraphicsContext.current
  return remember(size) {
    object : RememberObserver {
      val list = buildList {
        for (i in 0..<size) {
          add(graphicsContext.createGraphicsLayer())
        }
      }

      fun free() {
        for (layer in list) {
          graphicsContext.releaseGraphicsLayer(layer)
        }
      }

      override fun onAbandoned() {
        free()
      }

      override fun onForgotten() {
        free()
      }

      override fun onRemembered() {
        // NO-OP
      }
    }

  }.list

}
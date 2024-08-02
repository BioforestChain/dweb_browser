package org.dweb_browser.helper.capturable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun Modifier.capturable2(controller: CaptureV2Controller) = this.composed {
  when (val task = controller.taskFlow.collectAsState().value) {
    null -> this
    else -> {
      val scope = rememberCoroutineScope()
      val graphicsLayer = rememberGraphicsLayer()
      this.drawWithContent {
        // call record to capture the content in the graphics layer
        graphicsLayer.record {
          // draw the contents of the composable into the graphics layer
          this@drawWithContent.drawContent()
        }
        // draw the graphics layer on the visible canvas
        drawLayer(graphicsLayer)
        scope.launch {
          task.complete(graphicsLayer.toImageBitmap())
        }
      }
    }
  }
}

@Composable
fun rememberCaptureV2Controller(): CaptureV2Controller {
  return remember { CaptureV2Controller() }
}

class CaptureV2Controller : SynchronizedObject() {
  val taskFlow = MutableStateFlow<CompletableDeferred<ImageBitmap>?>(null)
  fun captureAsync() = synchronized(this) {
    taskFlow.value ?: CompletableDeferred<ImageBitmap>().also { task ->
      task.invokeOnCompletion {
        if (taskFlow.value == task) {
          taskFlow.value = null
        }
      }
      taskFlow.value = task
    }
  }

  suspend fun capture() = captureAsync().await()
}
package org.dweb_browser.helper.capturable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.globalEmptyScope

fun Modifier.capturable2(controller: CaptureV2Controller) = this.composed {
  when (val task = controller.taskFlow.collectAsState().value) {
    null -> this
    else -> {
      val ready by controller.readyFlow.collectAsState()
      if (!ready) {
        return@composed this
      }
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
  internal val taskFlow = MutableStateFlow<CompletableDeferred<ImageBitmap>?>(null)
  internal val readyFlow = MutableStateFlow(true)
  private val captureStartFlow = MutableSharedFlow<Unit?>()
  val onCaptureStart =
    captureStartFlow.filterNotNull().shareIn(globalDefaultScope, started = SharingStarted.Eagerly)

  private val captureEndFlow = MutableSharedFlow<Unit?>()
  val onCaptureEnd = captureEndFlow.asSharedFlow()
  fun captureAsync() = synchronized(this) {
    taskFlow.value ?: CompletableDeferred<ImageBitmap>().also { task ->
      task.invokeOnCompletion {
        globalEmptyScope.launch(start = CoroutineStart.UNDISPATCHED) {
          if (taskFlow.value == task) {
            taskFlow.value = null
          }
          captureEndFlow.emit(Unit)
        }
      }
      globalEmptyScope.launch(start = CoroutineStart.UNDISPATCHED) {
        readyFlow.value = false
        taskFlow.value = task
        captureStartFlow.emit(Unit)
        captureStartFlow.emit(null)
        readyFlow.value = true
      }
    }
  }

  suspend fun capture() = captureAsync().await()
}
package org.dweb_browser.helper.capturable

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Adds a capture-ability on the Composable which can draw Bitmap from the Composable component.
 *
 * Example usage:
 *
 * ```
 *  val captureController = rememberCaptureController()
 *  val uiScope = rememberCoroutineScope()
 *
 *  // The content to be captured in to Bitmap
 *  Column(
 *      modifier = Modifier.capturable(captureController),
 *  ) {
 *      // Composable content
 *  }
 *
 *  Button(onClick = {
 *      // Capture content
 *      val bitmapAsync = captureController.captureAsync()
 *      try {
 *          val bitmap = bitmapAsync.await()
 *          // Do something with `bitmap`.
 *      } catch (error: Throwable) {
 *          // Error occurred, do something.
 *      }
 *  }) { ... }
 * ```
 *
 * @param controller A [CaptureController] which gives control to capture the Composable content.
 */
@ExperimentalComposeUiApi
fun Modifier.capturable(controller: CaptureController): Modifier {
  return this then CapturableModifierNodeElement(controller)
}

/**
 * Modifier implementation of Capturable
 */
internal data class CapturableModifierNodeElement(
  private val controller: CaptureController
) : ModifierNodeElement<CapturableModifierNode>() {
  override fun create(): CapturableModifierNode {
    return CapturableModifierNode(controller)
  }

  override fun update(node: CapturableModifierNode) {
    node.updateController(controller)
  }
}

/**
 * Capturable Modifier node which delegates task to the [CacheDrawModifierNode] for drawing in
 * runtime when content capture is requested
 * [CacheDrawModifierNode] is used for drawing Composable UI from Canvas to the Picture and then
 * this node converts picture into a Bitmap.
 *
 * @param controller A [CaptureController] which gives control to capture the Composable content.
 */
@Suppress("unused")
internal class CapturableModifierNode(
  controller: CaptureController
) : DelegatingNode(), DelegatableNode {

  /**
   * State to hold the current [CaptureController] instance.
   * This can be updated via [updateController] method.
   */
  private val currentController = MutableStateFlow(controller)

  override fun onAttach() {
    super.onAttach()
    coroutineScope.launch {
      observeCaptureRequestsAndServe()
    }
  }

  /**
   * Sets new [CaptureController]
   */
  fun updateController(newController: CaptureController) {
    currentController.value = newController
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun observeCaptureRequestsAndServe() {
    currentController
      .flatMapLatest { it.captureRequests }
      .collect { request ->
        val completable = request.imageBitmapDeferred
        try {
          completable.complete(getCurrentContentAsImageBitmap(this, request.config))
        } catch (error: Throwable) {
          completable.completeExceptionally(error)
        }
      }
  }

  internal fun drawDelegate(onBuildDrawCache: CacheDrawScope.() -> DrawResult): () -> Unit {
    val node = delegate(CacheDrawModifierNode(onBuildDrawCache))
    return {
      undelegate(node)
    }
  }
}

internal expect suspend fun getCurrentContentAsImageBitmap(
  node: CapturableModifierNode,
  config: CaptureController.CaptureConfig
): ImageBitmap
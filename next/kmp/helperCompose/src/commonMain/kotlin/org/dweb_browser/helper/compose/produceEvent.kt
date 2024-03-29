package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Queue

@Composable
fun produceEvent(
  key1: Any? = null,
  key2: Any? = null,
  key3: Any? = null,
  scope: CoroutineScope = rememberCoroutineScope(),
  handler: suspend () -> Any,
): () -> Unit {
  val eventHandler = remember(key1, key2, key3) {

    val queueHandler = Queue.drop(handler);
    {
      scope.launch {
        queueHandler()
      }
      Unit
    }
  }
  return eventHandler
}
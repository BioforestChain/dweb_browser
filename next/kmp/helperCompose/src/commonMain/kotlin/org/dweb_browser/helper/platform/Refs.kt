package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.randomUUID

class Refs(val timeout: Long = 1000, val onUnRef: () -> Unit) {
  private val reasons = SafeHashSet<String>()
  fun addReason(reason: String) {
    reasons.add(reason)
  }

  @Composable
  fun RefEffect() {
    val reason = remember { randomUUID() }
    DisposableEffect(reason) {
      addReason(reason)
      onDispose {
        removeReason(reason)
      }
    }
  }

  fun removeReason(reason: String) {
    reasons.remove(reason)
    globalDefaultScope.launch {
      delay(timeout)
      if (reasons.isEmpty()) {
        onUnRef()
      }
    }
  }
}
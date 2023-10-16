package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReasonLock {
  val reasons = SafeHashMap<String, Mutex>()
  suspend inline fun <T> withLock(reason: String, block: () -> T) =
    reasons.getOrPut(reason) { Mutex() }.withLock(action = block)
}
package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutexLock = Mutex()
private var job: Job? = null
suspend fun <T> debounce(
  scope: CoroutineScope,
  action: suspend () -> T,
  interval: Long = 500L,
) = mutexLock.withLock {
  if (job?.isActive != true) {
    job = scope.launch(Dispatchers.Default) {
      delay(interval)
      action()
    }
  }
}
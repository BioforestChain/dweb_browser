package org.dweb_browser.sys.window.ext


import io.ktor.http.HttpMethod
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.WeakHashMap

private val microModuleModalLocks = WeakHashMap<MicroModule, Mutex>()
private val microModuleModalLocksSyncObj = SynchronizedObject()

val NativeMicroModule.modalLock
  get() = synchronized(microModuleModalLocksSyncObj) {
    microModuleModalLocks.getOrPut(
      this
    ) { Mutex() }
  }

suspend fun NativeMicroModule.createAlert() = modalLock.withLock {
  val router = routes();
  router.addRoutes("/internal/callback" bind HttpMethod.Get to defineEmptyResponse {
    removeRouter(router)
  })
}

@Serializable
data class AlertOptions(
  val title: String,
  val message: String,
  val iconUrl: String? = null,
  val iconAlt: String? = null,
  val confirmText: String? = null,
  val dismissText: String? = null,
)

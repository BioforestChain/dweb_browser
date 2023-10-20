package org.dweb_browser.sys.window.ext


import io.ktor.http.HttpMethod
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.decodeTo
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.BottomSheetsModal
import org.dweb_browser.sys.window.core.WindowRenderProvider
import org.dweb_browser.sys.window.core.windowAdapterManager

private val microModuleModalLocks = WeakHashMap<MicroModule, Mutex>()
private val microModuleModalLocksSyncObj = SynchronizedObject()

val NativeMicroModule.modalLock: Mutex
  get() = synchronized(microModuleModalLocksSyncObj) {
    microModuleModalLocks.getOrPut(
      this
    ) { Mutex() }
  }

suspend fun NativeMicroModule.createBottomSheets(renderProvider: WindowRenderProvider) =
  modalLock.withLock {
    val callbackRouter = routes();
    val callbackUrlId = randomUUID()
    val callbackUrlPathname = "/internal/callback/confirm-alert/$callbackUrlId"
    val onDismiss = CompletableDeferred<Unit>()
    callbackRouter.addRoutes(callbackUrlPathname bind HttpMethod.Get to defineEmptyResponse {
      onDismiss.complete(Unit)
      removeRouter(callbackRouter)
    });
    val callbackUrl = "file://$mmid$callbackUrlPathname"

    val bottomSheetsModal =
      nativeFetch("file://window.sys.dweb/createBottomSheets?dismissCallbackUrl=${callbackUrl.encodeURIComponent()}").body.toPureString()
        .decodeTo<BottomSheetsModal>()
    windowAdapterManager.provideRender(bottomSheetsModal.renderId, renderProvider)
    BottomSheets(bottomSheetsModal, onDismiss, this)
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

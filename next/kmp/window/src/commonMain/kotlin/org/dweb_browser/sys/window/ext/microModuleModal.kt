package org.dweb_browser.sys.window.ext


import io.ktor.http.HttpMethod
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.decodeTo
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.window.core.AlertModal.Companion.createAlertModal
import org.dweb_browser.sys.window.core.BottomSheetsModal.Companion.createBottomSheetsModal
import org.dweb_browser.sys.window.core.ModalCallback
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

suspend fun NativeMicroModule.createBottomSheets(
  title: String? = null,
  iconUrl: String? = null,
  iconAlt: String? = null,
  renderProvider: WindowRenderProvider
) = modalLock.withLock {
  val callbackRouter = routes();
  val callbackUrlId = randomUUID()
  val callbackUrlPathname = "/internal/callback/bottom-sheets/$callbackUrlId"
  val onCallback = MutableSharedFlow<ModalCallback>()
  callbackRouter.addRoutes(callbackUrlPathname bind HttpMethod.Post to defineEmptyResponse {
    onCallback.emit(request.body.toPureString().decodeTo())
  });

  val mainWindow = getMainWindow()
  val modal = mainWindow.createBottomSheetsModal(
    title = title,
    iconUrl = iconUrl,
    iconAlt = iconAlt,
    callbackUrl = "file://$mmid$callbackUrlPathname",
  )

  windowAdapterManager.provideRender(modal.renderId, renderProvider)
  WindowBottomSheetsController(this, modal, onCallback.asSharedFlow()).also { controller ->
    controller.onDestroy {
      removeRouter(callbackRouter)
    }
  }
}

suspend fun NativeMicroModule.createAlert(
  title: String,
  message: String,
  iconUrl: String? = null,
  iconAlt: String? = null,
  confirmText: String? = null,
  dismissText: String? = null,
) = modalLock.withLock {
  val callbackUrlId = randomUUID()
  val callbackUrlPathname = "/internal/callback/alert/$callbackUrlId"
  val onCallback = MutableSharedFlow<ModalCallback>()
  val callbackRouter = routes(callbackUrlPathname bind HttpMethod.Post to defineEmptyResponse {
    onCallback.emit(request.body.toPureString().decodeTo())
  });
  val mainWindow = getMainWindow()
  val modal = mainWindow.createAlertModal(
    title = title,
    message = message,
    iconUrl = iconUrl,
    iconAlt = iconAlt,
    confirmText = confirmText,
    dismissText = dismissText,
    callbackUrl = "file://$mmid$callbackUrlPathname",
  )
  WindowAlertController(this, modal, onCallback.asSharedFlow()).also { controller ->
    controller.onDestroy {
      removeRouter(callbackRouter)
    }
  }
}
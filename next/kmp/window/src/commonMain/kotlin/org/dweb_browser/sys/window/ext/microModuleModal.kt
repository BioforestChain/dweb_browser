package org.dweb_browser.sys.window.ext


import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.decodeTo
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.WindowRenderProvider
import org.dweb_browser.sys.window.core.modal.AlertModalState.Companion.createAlertModal
import org.dweb_browser.sys.window.core.modal.BottomSheetsModalState.Companion.createBottomSheetsModal
import org.dweb_browser.sys.window.core.modal.ModalCallback
import org.dweb_browser.sys.window.core.modal.WindowAlertController
import org.dweb_browser.sys.window.core.modal.WindowBottomSheetsController
import org.dweb_browser.sys.window.core.windowAdapterManager

private val microModuleModalLocks = WeakHashMap<MicroModule.Runtime, Mutex>()
private val microModuleModalLocksSyncObj = SynchronizedObject()

private val NativeMicroModule.NativeRuntime.modalLock: Mutex
  get() = synchronized(microModuleModalLocksSyncObj) {
    microModuleModalLocks.getOrPut(
      this
    ) { Mutex() }
  }

suspend fun NativeMicroModule.NativeRuntime.createBottomSheets(
  title: String? = null,
  iconUrl: String? = null,
  iconAlt: String? = null,
  wid: String? = null,
  renderProvider: WindowRenderProvider,
): WindowBottomSheetsController {
  val callbackRouter = routes();
  val callbackUrlId = randomUUID()
  val callbackUrlPathname = "/internal/callback/bottom-sheets/$callbackUrlId"
  val onCallback = MutableSharedFlow<ModalCallback>()
  callbackRouter.addRoutes(callbackUrlPathname bind PureMethod.POST by defineEmptyResponse {
    onCallback.emit(request.body.toPureString().decodeTo())
  });
  // TODO
  val mainWindow = getWindow(wid ?: getOrOpenMainWindowId())
  val modal = mainWindow.createBottomSheetsModal(
    title = title,
    iconUrl = iconUrl,
    iconAlt = iconAlt,
    callbackUrl = "file://$mmid$callbackUrlPathname",
  )

  windowAdapterManager.provideRender(modal.renderId, renderProvider)
  return WindowBottomSheetsController(
    this,
    modal,
    mainWindow.id,
    onCallback.asSharedFlow()
  ).also { controller ->
    controller.onDestroy {
      removeRouter(callbackRouter)
    }
  }
}

suspend fun NativeMicroModule.NativeRuntime.createAlert(
  title: String,
  message: String,
  iconUrl: String? = null,
  iconAlt: String? = null,
  confirmText: String? = null,
  dismissText: String? = null,
  wid: String? = null,
): WindowAlertController {
  val callbackUrlId = randomUUID()
  val callbackUrlPathname = "/internal/callback/alert/$callbackUrlId"
  val onCallback = MutableSharedFlow<ModalCallback>()
  val callbackRouter = routes(callbackUrlPathname bind PureMethod.POST by defineEmptyResponse {
    onCallback.emit(request.body.toPureString().decodeTo())
  });

  val mainWindow = getWindow(wid ?: getOrOpenMainWindowId())
  val modal = mainWindow.createAlertModal(
    title = title,
    message = message,
    iconUrl = iconUrl,
    iconAlt = iconAlt,
    confirmText = confirmText,
    dismissText = dismissText,
    callbackUrl = "file://$mmid$callbackUrlPathname",
  )
  return WindowAlertController(
    this,
    modal,
    mainWindow.id,
    onCallback.asSharedFlow()
  ).also { controller ->
    controller.onDestroy {
      removeRouter(callbackRouter)
    }
  }
}
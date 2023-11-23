package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import platform.UIKit.UIViewController

data class DwebUIViewControllerProperty(val vcId: Int, val zIndex: Int, val visible: Boolean)

private var ios_rootUIViewControllerAddHook: (UIViewController, DwebUIViewControllerProperty) -> Unit =
  { _, _ -> }

val nativeRootUIViewController_scope = CoroutineScope(defaultAsyncExceptionHandler);
fun nativeRootUIViewController_setAddHook(hook: (UIViewController, DwebUIViewControllerProperty) -> Unit) {
  ios_rootUIViewControllerAddHook = hook
}

private var ios_rootUIViewControllerUpdateHook: (DwebUIViewControllerProperty) -> Unit = {}

fun nativeRootUIViewController_setUpdateHook(hook: (DwebUIViewControllerProperty) -> Unit) {
  ios_rootUIViewControllerUpdateHook = hook
}

private var ios_rootUIViewControllerRemoveHook: (vcId: Int) -> Unit = {}

fun nativeRootUIViewController_setRemoveHook(hook: (vcId: Int) -> Unit) {
  ios_rootUIViewControllerRemoveHook = hook
}


private val allVcs = SafeHashSet<PureViewController>()
fun getNativeVcMaxZIndex() =
  allVcs.reduceOrNull { a, b -> if (a.prop.zIndex > b.prop.zIndex) a else b }?.prop?.zIndex ?: 0

val nativeRootUIViewController_onInitSignal = Signal<Int>();
val nativeRootUIViewController_onDestroySignal = Signal<Int>();
private val vscLock = Mutex()

suspend fun nativeRootUIViewController_addOrUpdate(
  pureViewController: PureViewController, zIndex: Int? = null, visible: Boolean = true
) = vscLock.withLock {
  fun updateProp() = pureViewController.prop.copy(
    zIndex = zIndex ?: getNativeVcMaxZIndex() + 1, visible = visible
  ).also { pureViewController.prop = it }

  coroutineScope {
    val prop = updateProp()
    if (pureViewController.isInit) {
      ios_rootUIViewControllerUpdateHook(prop)
    } else {
      /// 设置init监听，等待vc构建完成
      val waitInit = async { pureViewController.waitInit() }
      // 唤醒swift进行重新绘制一个新的UIViewController
      ios_rootUIViewControllerAddHook(pureViewController.uiViewController, prop)
      waitInit.await()
    }
  }
}

suspend fun nativeRootUIViewController_remove(pureViewController: PureViewController) =
  vscLock.withLock {
    if (pureViewController.isInit) {
      ios_rootUIViewControllerRemoveHook(pureViewController.vcId)
    }
  }
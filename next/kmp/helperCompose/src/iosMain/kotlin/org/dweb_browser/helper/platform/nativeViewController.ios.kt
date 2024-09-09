package org.dweb_browser.helper.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.mainAsyncExceptionHandler
import platform.UIKit.UIViewController

data class DwebUIViewControllerProperty(
  val vcId: Int,
  val zIndex: Int,
  val visible: Boolean,
  val fullscreen: Boolean,
)

private var addHook: (UIViewController, DwebUIViewControllerProperty) -> Unit =
  { _, _ -> }
private var updateHook: (DwebUIViewControllerProperty) -> Unit = {}
private var removeHook: (vcId: Int) -> Unit = {}
private var navigationBarHook: (visible: Boolean) -> Unit = {}
private var updateEdgeSwipeSwitch: (enable: Boolean) -> Unit = {}

@Suppress("unused")
class NativeViewController private constructor() {
  companion object {
    val nativeViewController = NativeViewController();
  }

  val scope = CoroutineScope(mainAsyncExceptionHandler)
  val navigationBar get() = navigationBarHook

  fun setNavigationBarHook(hook: (visible: Boolean) -> Unit) {
    navigationBarHook = hook
  }

  private val onGoBackSignal = SimpleSignal();
  val onGoBack = onGoBackSignal.toListener()
  fun emitOnGoBack() {
    scope.launch {
      onGoBackSignal.emit()
    }
  }

  fun setAddHook(hook: (UIViewController, DwebUIViewControllerProperty) -> Unit) {
    addHook = hook
  }

  fun setUpdateHook(hook: (DwebUIViewControllerProperty) -> Unit) {
    updateHook = hook
  }

  fun setRemoveHook(hook: (vcId: Int) -> Unit) {
    removeHook = hook
  }

  fun setUpdateSwitchHook(cb: (enable: Boolean) -> Unit) {
    updateEdgeSwipeSwitch = cb
  }

  private val allVcs = SafeHashSet<PureViewController>()
  private fun getMaxZIndex() =
    allVcs.reduceOrNull { a, b -> if (a.prop.zIndex > b.prop.zIndex) a else b }?.prop?.zIndex ?: 0

  internal val onInitSignal = Signal<Int>();
  fun emitOnInit(vcId: Int) {
    scope.launch {
      onInitSignal.emit(vcId)
    }
  }

  internal val onDestroySignal = Signal<Int>();
  fun emitOnDestroy(vcId: Int) {
    scope.launch {
      onDestroySignal.emit(vcId)
    }
  }

  private val vscLock = Mutex()

  suspend fun addOrUpdate(
    pureViewController: PureViewController, zIndex: Int? = null, visible: Boolean = true,
  ) = vscLock.withLock {
    fun updateProp() = pureViewController.prop.copy(
      zIndex = zIndex ?: (getMaxZIndex() + 1), visible = visible
    ).also { pureViewController.prop = it }

    val prop = updateProp()
    if (pureViewController.isAdded) {
      updateHook(prop)
    } else {
      // 唤醒swift进行重新绘制一个新的UIViewController
      addHook(pureViewController.getUiViewController(), prop)
      /// 设置init监听，等待vc构建完成
      pureViewController.waitInit()
    }
  }

  suspend fun remove(pureViewController: PureViewController) = vscLock.withLock {
    if (pureViewController.isAdded) {
      removeHook(pureViewController.vcId)
    }
  }

  fun updateEdgeSwipeEnable(enable: Boolean){
    updateEdgeSwipeSwitch(enable)
  }
}


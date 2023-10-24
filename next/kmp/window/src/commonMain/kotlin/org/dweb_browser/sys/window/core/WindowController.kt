package org.dweb_browser.sys.window.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.sys.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.core.helper.setDefaultFloatWindowBounds

abstract class WindowController(
  /**
   * 窗口的基本信息
   */
  val state: WindowState,
  /**
   * 传入多窗口管理器，可以不提供，那么由 Controller 自身以缺省的逻辑对 WindowState 进行修改
   */
  manager: WindowsManager<*>? = null,
) {
  protected var _manager: WindowsManager<*>? = null

  /**
   * 窗口管理器
   * 默认情况下，WindowController 的接口只对 Manager 开放，所以如果有需要，请调用 manager 的接口来控制窗口
   */
  open val manager get() = _manager
  open fun upsetManager(manager: WindowsManager<*>?) {
    _manager = manager
  }

  private suspend fun <R> managerRunOr(
    withManager: (manager: WindowsManager<*>) -> Deferred<R>, orNull: suspend () -> R
  ) = when (_manager) {
    null -> orNull()
    else -> withManager(_manager!!).await()
  }

  init {
    this.upsetManager(manager)
  }

  /**
   * 在Android中，一个窗口对象必然附加在某一个Context/Activity中
   */
  abstract val viewController: PlatformViewController

  /**
   * 需要提供一个生命周期对象
   */
  abstract val coroutineScope: CoroutineScope
  val id = state.constants.wid;

  //#region WindowMode相关的控制函数
  private fun <R> createStateListener(
    key: WindowPropertyKeys,
    filter: (WindowState.(Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
    map: (Observable.Change<WindowPropertyKeys, *>) -> R
  ) = state.observable.changeSignal.createChild(
    { (it.key == key) && (filter?.invoke(state, it) != false) }, map
  ).toListener()

  val onFocus =
    createStateListener(WindowPropertyKeys.Focus, { focus }) { debugWindow("emit onFocus", id) }
  val onBlur =
    createStateListener(WindowPropertyKeys.Focus, { !focus }) { debugWindow("emit onBlur", id) }

  fun isFocused() = state.focus
  internal open suspend fun simpleFocus() {
    state.focus = true
    // 如果窗口聚焦，那么同时要确保可见性
    simpleToggleVisible(true)
  }

  suspend fun focus() = managerRunOr({ it.focusWindow(this) }, { simpleFocus() })


  internal open suspend fun simpleBlur() {
    state.focus = false
  }

  suspend fun blur() = managerRunOr({ it.blurWindow(this) }, { simpleBlur() })

  val onModeChange =
    createStateListener(WindowPropertyKeys.Mode) { debugWindow("emit onModeChange", "$id $it") };

  fun isMaximized(mode: WindowMode = state.mode) =
    mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN

  val onMaximize = createStateListener(WindowPropertyKeys.Mode,
    { isMaximized(mode) }) { debugWindow("emit onMaximize", id) }

  internal open suspend fun simpleMaximize() {
    if (!isMaximized()) {
      _beforeMaximizeBounds = state.bounds.copy()
      state.mode = WindowMode.MAXIMIZE
    }
  }

  suspend fun maximize() = managerRunOr({ it.maximizeWindow(this) }, { simpleMaximize() })

  private var _beforeMaximizeBounds: Rect? = null

  /**
   * 记忆窗口最大化之前的记忆
   */
  val beforeMaximizeBounds get() = _beforeMaximizeBounds;


  /**
   * 当窗口从最大化状态退出时触发
   * Emitted when the window exits from a maximized state.
   */
  val onUnMaximize = createStateListener(WindowPropertyKeys.Mode, {
    !isMaximized(mode) && isMaximized(it.oldValue as WindowMode)
  }) { debugWindow("emit onUnMaximize", id) }

  /**
   * 取消窗口最大化
   */
  internal open suspend fun simpleUnMaximize() {
    if (isMaximized()) {
      when (val value = _beforeMaximizeBounds) {
        null -> {
          state.setDefaultFloatWindowBounds(
            state.bounds.width, state.bounds.height, state.zIndex.toFloat(), true
          )
        }

        else -> {
          state.bounds = value
          _beforeMaximizeBounds = null
        }
      }
      state.mode = WindowMode.FLOAT
    }
  }

  suspend fun unMaximize() = managerRunOr({ it.unMaximizeWindow(this) }, { simpleUnMaximize() })

  fun isVisible() = state.visible

  val onVisible = createStateListener(WindowPropertyKeys.Visible, { visible }) {
    debugWindow(
      "emit onVisible", id
    )
  }
  val onHidden = createStateListener(WindowPropertyKeys.Visible, { !visible }) {
    debugWindow(
      "emit onHidden", id
    )
  }

  internal open suspend fun simpleToggleVisible(visible: Boolean? = null) {
    val value = visible ?: !state.visible
    if (value != state.visible) {
      state.visible = value
    }
  }

  suspend fun toggleVisible(visible: Boolean? = null) =
    managerRunOr({ it.toggleVisibleWindow(this, visible) }, { simpleToggleVisible(visible) })

  val onClose = createStateListener(WindowPropertyKeys.Mode,
    { mode == WindowMode.CLOSE }) { debugWindow("emit onClose", id) }

  fun isClosed() = state.mode == WindowMode.CLOSE
  internal open suspend fun simpleClose(force: Boolean = false) {
    if (!force) {
      /// 如果有关闭提示，并且没有显示，那么就显示一下
      if (state.closeTip != null && !state.showCloseTip) {
        state.showCloseTip = true
        return
      }
    }
    state.mode = WindowMode.CLOSE
  }

  suspend fun close(force: Boolean = false) =
    managerRunOr({ it.closeWindow(this, force) }, { simpleClose(force) })

//#endregion

  //#region 窗口样式修饰
  internal open suspend fun simpleSetStyle(style: WindowStyle) {
    with(style) {
      topBarOverlay?.also { state.topBarOverlay = it }
      bottomBarOverlay?.also { state.bottomBarOverlay = it }
      topBarContentColor?.also { state.topBarContentColor = it }
      topBarContentDarkColor?.also { state.topBarContentDarkColor = it }
      topBarBackgroundColor?.also { state.topBarBackgroundColor = it }
      topBarBackgroundDarkColor?.also { state.topBarBackgroundDarkColor = it }
      bottomBarContentColor?.also { state.bottomBarContentColor = it }
      bottomBarContentDarkColor?.also { state.bottomBarContentDarkColor = it }
      bottomBarBackgroundColor?.also { state.bottomBarBackgroundColor = it }
      bottomBarBackgroundDarkColor?.also { state.bottomBarBackgroundDarkColor = it }
      bottomBarTheme?.also { state.bottomBarTheme = WindowBottomBarTheme.from(it) }
      themeColor?.also { state.themeColor = it }
      themeDarkColor?.also { state.themeDarkColor = it }
    }
  }

  suspend fun setStyle(style: WindowStyle) = managerRunOr({
    it.windowSetStyle(
      this, style
    )
  }, { simpleSetStyle(style) })

  //#endregion
  private val goBackSignal = SimpleSignal()
  val onGoBack = goBackSignal.toListener()

  @Composable
  fun GoBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    DisposableEffect(this, enabled) {
      state.canGoBack = enabled
      val off = goBackSignal.listen { if (enabled) onBack() }
      onDispose {
        state.canGoBack = null
        off()
      }
    }
  }

  internal open suspend fun simpleEmitGoBack() {
    goBackSignal.emit()
  }

  suspend fun emitGoBack() = managerRunOr({ it.windowEmitGoBack(this) }, { simpleEmitGoBack() })


  private val goForwardSignal = SimpleSignal()
  val onGoForward = goForwardSignal.toListener()

  @Composable
  fun GoForwardHandler(enabled: Boolean = true, onForward: () -> Unit) {
    state.canGoForward = enabled
    DisposableEffect(this, enabled) {
      val off = goForwardSignal.listen { if (enabled) onForward() }
      onDispose {
        state.canGoForward = null
        off()
      }
    }
  }


  internal open suspend fun simpleEmitGoForward() {
    goForwardSignal.emit()
  }

  suspend fun emitGoForward() =
    managerRunOr({ it.windowEmitGoForward(this) }, { simpleEmitGoForward() })


  internal open fun simpleHideCloseTip() {
    state.showCloseTip = false
  }

  suspend fun hideCloseTip() =
    managerRunOr({ it.windowHideCloseTip(this) }, { simpleHideCloseTip() })

  internal open fun simpleToggleMenuPanel(show: Boolean?) {
    state.showMenuPanel = show ?: !state.showMenuPanel
  }

  suspend fun toggleMenuPanel(show: Boolean? = null) =
    managerRunOr({ it.windowToggleMenuPanel(this, show) }, { simpleToggleMenuPanel(show) })

  suspend fun hideMenuPanel() = toggleMenuPanel(false)

  suspend fun showMenuPanel() = toggleMenuPanel(true)

  internal open suspend fun simpleToggleAlwaysOnTop(onTop: Boolean? = null) {
    state.alwaysOnTop = onTop ?: !state.alwaysOnTop
  }

  suspend fun toggleAlwaysOnTop(onTop: Boolean? = null) =
    managerRunOr({ it.windowToggleAlwaysOnTop(this, onTop) }, { simpleToggleAlwaysOnTop(onTop) })

  suspend fun disableAlwaysOnTop() = toggleAlwaysOnTop(false)

  suspend fun enableAlwaysOnTop() = toggleAlwaysOnTop(true)

  internal open suspend fun simpleToggleColorScheme(colorScheme: WindowColorScheme? = null) {
    state.colorScheme = colorScheme ?: state.colorScheme.next()
  }

  suspend fun toggleColorScheme(colorScheme: WindowColorScheme? = null) =
    managerRunOr({ it.windowToggleColorScheme(this, colorScheme) },
      { simpleToggleColorScheme(colorScheme) })

  private val modalsLock = ReasonLock()


  /**
   * 尝试添加一个 modal
   * 如果ID已经存在，返回成功
   */
  suspend fun saveModal(modal: ModalState) = modalsLock.withLock("write") {
    if (!state.modals.containsKey(modal.modalId)) {
      modal.setParent(this)
    }
  }

  /**
   * 尝试移除一个 modal
   */
  suspend fun removeModal(modalId: UUID) = modalsLock.withLock("write") {
    when (val modal = state.modals[modalId]) {
      null -> false
      else -> {
        state.modals -= modalId
        windowAdapterManager.renderProviders.remove(modal.renderId)
        true
      }
    }
  }

  /**
   * 取当前正在显示的 modal
   */
  private suspend fun getOpenModal() = modalsLock.withLock("read") {
    state.modals.firstNotNullOfOrNull { if (it.value.isOpen.value) it.value else null }
  }

  val openingModal = mutableStateOf<ModalState?>(null)

  /**
   * 尝试显示指定 modal
   *
   * @return 返回true说明指定 modal 已经在显示了
   */
  suspend fun openModal(modalId: String) = modalsLock.withLock("write") {
    val modal = state.modals[modalId] ?: return@withLock false
    /// 找寻当前
    when (getOpenModal()) {
      modal -> true
      null -> {
        modal.isOpen.value = true
        openingModal.value = modal
        true
      }

      else -> false
    }
  }

  /**
   * 尝试关闭指定 modal
   *
   * @return 返回true说明这次操作让这个 modal 关闭了。否则可能是 modal不存在、或者modal本来就是关闭的
   */
  suspend fun closeModal(modalId: UUID) = modalsLock.withLock("write") {
    val modal = state.modals[modalId] ?: return@withLock false
    if (modal.isOpen.value) {
      modal.isOpen.value = false
      openingModal.value = getOpenModal()
      true
    } else false
  }
}
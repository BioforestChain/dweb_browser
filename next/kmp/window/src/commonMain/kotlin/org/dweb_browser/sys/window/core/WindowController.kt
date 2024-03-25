package org.dweb_browser.sys.window.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.core.helper.setDefaultFloatWindowBounds
import org.dweb_browser.sys.window.core.modal.ModalState

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

  internal val _pureViewControllerState = MutableStateFlow<IPureViewController?>(null)
  val pureViewControllerState: StateFlow<IPureViewController?> get() = _pureViewControllerState
  val pureViewController get() = pureViewControllerState.value

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
  abstract val viewBox: IPureViewBox

  /**
   * 需要提供一个生命周期对象
   */
  abstract val lifecycleScope: CoroutineScope
  val id = state.constants.wid;

  //#region WindowMode相关的控制函数
  private fun <R> createStateListener(
    key: WindowPropertyKeys,
    filter: (WindowState.(Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
    map: (Observable.Change<WindowPropertyKeys, *>) -> R
  ) = state.observable.changeSignal.createChild(
    {
      if ((it.key == key) && (filter?.invoke(state, it) != false)) it else null
    }, map
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

  val focusRequester = FocusRequester().also { focusRequester ->
    onFocus {
      try {
        focusRequester.requestFocus()
      } catch (e: Throwable) {
        // ignore FocusRequesterNotInitialized error
        if (e.message?.contains("FocusRequester is not initialized") != true) {
          throw e
        }
      }
    }
  }

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

  private var _beforeMaximizeBounds: PureRect? = null

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
          state.updateBounds(value, WindowState.UpdateBoundsReason.Inner)
          _beforeMaximizeBounds = null
        }
      }
      state.mode = WindowMode.FLOAT
    }
  }

  suspend fun unMaximize() = managerRunOr({ it.unMaximizeWindow(this) }, { simpleUnMaximize() })

  fun isVisible() = state.visible

  val onVisible = createStateListener(WindowPropertyKeys.Visible, { visible }) {
    debugWindow("emit onVisible", id)
  }
  val onHidden = createStateListener(WindowPropertyKeys.Visible, { !visible }) {
    debugWindow("emit onHidden", id)
  }

  internal open suspend fun simpleToggleVisible(visible: Boolean? = null) {
    val value = visible ?: !state.visible
    if (value != state.visible) {
      state.visible = value
    }
  }

  suspend fun toggleVisible(visible: Boolean? = null) =
    managerRunOr({ it.toggleVisibleWindow(this, visible) }, { simpleToggleVisible(visible) })

  suspend fun show() = toggleVisible(true)
  suspend fun hide() = toggleVisible(false)

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

  /**
   * 关闭窗口
   * 一般来来说不要直接使用该核心接口，请使用 tryCloseOrHide 、 closeRoot 等业务接口进行替代
   */
  @LowLevelWindowAPI
  suspend fun close(force: Boolean = false) =
    managerRunOr({ it.closeWindow(this, force) }, { simpleClose(force) })

  val isMainWindow get() = state.parent == null

  /**
   * 尝试关闭或者将窗口隐藏
   * 如果有父窗口，那么采用关闭
   * 如果自己就是主窗口，那么采用隐藏
   */
  @OptIn(LowLevelWindowAPI::class)
  suspend fun tryCloseOrHide(force: Boolean = false) {
    if (isMainWindow) hide()
    else close(force)
  }

  val mainWindow: WindowController
    get() {
      var root = this
      while (true) {
        when (val parentWid = state.parent) {
          null -> break;
          else -> {
            root = windowInstancesManager.get(parentWid) ?: break
          }
        }
      }
      return root
    }

  /**
   * 关闭主窗口，就能退出应用
   */
  @OptIn(LowLevelWindowAPI::class)
  suspend fun closeRoot(force: Boolean = false) {
    mainWindow.close(force)
  }

  //#endregion

  //#region 窗口样式修饰
  internal open suspend fun simpleSetStyle(style: WindowStyle) {
    with(style) {
      topBarOverlay?.also { state.topBarOverlay = it }
      bottomBarOverlay?.also { state.bottomBarOverlay = it }
      keyboardOverlaysContent?.also { state.keyboardOverlaysContent = it }
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

  private class GoBackRecord private constructor(
    val onBack: suspend () -> Unit,
    var enabled: Boolean,
    var uiScope: CoroutineScope,
  ) {
    companion object {
      private val WM = WeakHashMap<suspend () -> Unit, GoBackRecord>()
      fun from(onBack: suspend () -> Unit, enabled: Boolean, uiScope: CoroutineScope) =
        WM.getOrPut(onBack) { GoBackRecord(onBack, enabled, uiScope) }.also {
          it.enabled = enabled
          it.uiScope = uiScope
        }
    }
  }

  private val onBackRecords by lazy {
    mutableStateListOf<GoBackRecord>().also { records ->
      onGoBack {
        records.lastOrNull { it.enabled }?.apply {
          uiScope.launch {
            onBack()
          }
        }
      }
    }
  }

  @Composable
  fun GoBackHandler(
    enabled: Boolean = true, onBack: suspend () -> Unit
  ) {
    val uiScope = rememberCoroutineScope()
    DisposableEffect(this, enabled, onBack) {
      val record = GoBackRecord.from(onBack, enabled, uiScope)
      onBackRecords.add(record)
      state.canGoBack = onBackRecords.size > 0
      onDispose {
        onBackRecords.remove(record)
        state.canGoBack = if (onBackRecords.isEmpty()) null else onBackRecords.any { it.enabled }
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

  internal open suspend fun simpleToggleKeepBackground(keepBackground: Boolean? = null) {
    state.keepBackground = keepBackground ?: !state.keepBackground
  }

  suspend fun toggleKeepBackground(keepBackground: Boolean? = null) =
    managerRunOr({ it.windowToggleKeepBackground(this, keepBackground) },
      { simpleToggleKeepBackground(keepBackground) })

  suspend fun disableKeepBackground() = toggleKeepBackground(false)

  suspend fun enableKeepBackground() = toggleKeepBackground(true)

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
      modal.initParent(this)
    }
  }

  /**
   * 尝试移除一个 modal
   */
  suspend fun removeModal(module: MicroModule, modalId: String) = modalsLock.withLock("write") {
    val modal = state.modals[modalId] ?: return@withLock false
    modal.safeDestroy(module)
  }

  suspend fun updateModalCloseTip(modalId: String, closeTip: String?) =
    modalsLock.withLock("write") {
      val modal = state.modals[modalId] ?: return@withLock false
      modal.closeTip = closeTip
      true
    }

  /**
   * 取当前正在显示的 modal
   */
  internal suspend fun getOpenModal() = modalsLock.withLock("read") {
    state.modals.firstNotNullOfOrNull { if (it.value.isOpen) it.value else null }
  }

  @Transient
  val openingModal = mutableStateOf<ModalState?>(null)

  //  @Transient
//  val openingModalStateFlow = MutableStateFlow<ModalState?>(null)
  internal suspend fun updateOpeningModal() {
    openingModal.value = getOpenModal()
//    openingModalStateFlow.emit(getOpenModal())
  }

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
      null -> modal.safeOpen()
      else -> false
    }
  }

  /**
   * 尝试关闭指定 modal
   *
   * @return 返回true说明这次操作让这个 modal 关闭了。否则可能是 modal不存在、或者modal本来就是关闭的
   */
  suspend fun closeModal(module: MicroModule, modalId: String) = modalsLock.withLock("write") {
    val modal = state.modals[modalId] ?: return@withLock false
    modal.safeClose(module)
  }
}
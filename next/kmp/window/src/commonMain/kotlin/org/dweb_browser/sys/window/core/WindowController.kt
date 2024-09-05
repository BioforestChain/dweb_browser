package org.dweb_browser.sys.window.core

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.core.constant.WindowMode
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.core.helper.WindowNavigation
import org.dweb_browser.sys.window.core.helper.setDefaultFloatWindowBounds
import org.dweb_browser.sys.window.core.modal.ModalState

open class WindowController(
  val state: WindowState,
  val viewBox: IPureViewBox,
) {
  /**
   * 需要提供一个生命周期对象
   */
  open val lifecycleScope = viewBox.lifecycleScope
  override fun toString(): String {
    return "Win(${state.title}@$id)"
  }

  internal val _pureViewControllerState = MutableStateFlow<IPureViewController?>(null)
  val pureViewControllerState: StateFlow<IPureViewController?> get() = _pureViewControllerState
  val pureViewController get() = pureViewControllerState.value

  val id = state.constants.wid;


  private var _manager: WindowsManager<*>? = null
  open fun getManager() = _manager

  open fun upsetManager(manager: WindowsManager<*>?) {
    _manager = manager
  }

  //#region Focus
  private fun <R> createStateListener(
    key: WindowPropertyKeys,
    filter: (WindowState.(Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
    map: (Observable.Change<WindowPropertyKeys, *>) -> R,
  ) = state.observable.changeSignal.createChild(
    {
      if ((it.key == key) && (filter?.invoke(state, it) != false)) it else null
    }, map
  ).toListener()

  val onFocus =
    createStateListener(WindowPropertyKeys.Focus, { focus }) { debugWindow("emit onFocus", this) }
  val onBlur =
    createStateListener(WindowPropertyKeys.Focus, { !focus }) { debugWindow("emit onBlur", this) }

  val isFocused get() = state.focus
  open suspend fun focus() {
    state.focus = true
    // 如果窗口聚焦，那么同时要确保可见性
    show()
  }

  fun focusInBackground() = lifecycleScope.launch { focus() }

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

  open suspend fun blur() {
    state.focus = false
  }


  //#endregion Focus

  val onModeChange =
    createStateListener(WindowPropertyKeys.Mode) { debugWindow("emit onModeChange", "$this $it") };


  //#region maximize

  val isMaximized get() = isMaximized()
  fun isMaximized(mode: WindowMode = state.mode) =
    mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN

//  fun isMinimized(mode: WindowMode = state.mode) =
//    mode == WindowMode.MINIMIZE

  fun isFullscreen(mode: WindowMode = state.mode) = mode == WindowMode.FULLSCREEN

  val onMaximize = createStateListener(WindowPropertyKeys.Mode,
    { isMaximized(mode) }) { debugWindow("emit onMaximize", this) }
//  val onMinimize = createStateListener(WindowPropertyKeys.Mode,
//    { isMinimized(mode) }) { debugWindow("emit onMinimize", this) }

  open suspend fun maximize() {
    if (!isMaximized()) {
      beforeMaximizeBounds = state.bounds.copy()
      state.mode = WindowMode.MAXIMIZE
    }
  }

//  open suspend fun minimize() {
//    if (!isMaximized()) {
//      beforeMinimizeMode = state.mode
//      state.mode = WindowMode.MINIMIZE
//    }
//  }


  suspend fun fullscreen() {
    if (!isFullscreen()) {
      state.mode = WindowMode.FULLSCREEN
    }
  }

  /**
   * 记忆窗口最大化之前的大小
   */
  var beforeMaximizeBounds: PureRect? = null
    private set
  var beforeMinimizeMode: WindowMode? = null
    private set


  /**
   * 当窗口从最大化状态退出时触发
   * Emitted when the window exits from a maximized state.
   */
  val onUnMaximize = createStateListener(WindowPropertyKeys.Mode, {
    !isMaximized(mode) && isMaximized(it.oldValue as WindowMode)
  }) { debugWindow("emit onUnMaximize", this) }

  /**
   * 取消窗口最大化
   */
  open suspend fun unMaximize() {
    if (isMaximized()) {
      // 看看有没有记忆了之前的窗口大小
      when (val value = beforeMaximizeBounds) {
        null -> {
          // TODO: 应该使用适配器去处理原生窗口和模拟窗口的窗口大小变更按钮
          if(!IPureViewController.isDesktop) {
            state.setDefaultFloatWindowBounds(
              state.bounds.width, state.bounds.height, state.zIndex.toFloat(), true
            )
          }
        }

        else -> {
          state.updateBounds(value, WindowState.UpdateReason.Inner)
          beforeMaximizeBounds = null
        }
      }
      // 将窗口变成浮动模式，会触发双向绑定
      state.mode = WindowMode.FLOAT
    }
  }


//  /**
//   * 取消窗口最小化
//   */
//   open suspend fun unMinimize() {
//    if (isMinimized()) {
//      // 看看有没有记忆了之前的窗口模式
//      when (val value = beforeMinimizeMode) {
//        null -> {
//          state.mode = WindowMode.FLOAT
//        }
//
//        else -> {
//          state.mode = value
//          beforeMinimizeMode = null
//        }
//      }
//    }
//  }
//


  //#endregion maximize

  //#region 设置窗口大小
  open suspend fun setBounds(bounds: SetWindowSize) {
    if (state.mode != WindowMode.FLOAT) {
      state.mode = WindowMode.FLOAT
    }
    state.resizable = bounds.resizable
    // 当有传递的时候再设置
    state.updateMutableBounds {
      if (bounds.width != null) {
        this.width = bounds.width
      }
      if (bounds.height != null) {
        this.height = bounds.height
      }
    }
  }

  //#endregion

  //# region visible
  val isVisible get() = state.visible

  val onVisible = createStateListener(WindowPropertyKeys.Visible, { visible }) {
    debugWindow("emit onVisible", this)
  }
  val onHidden = createStateListener(WindowPropertyKeys.Visible, { !visible }) {
    debugWindow("emit onHidden", this)
  }

  open suspend fun toggleVisible(visible: Boolean? = null) {
    val value = visible ?: !state.visible
    if (value != state.visible) {
      state.visible = value
    }
  }

  //#endregion

  //#region close

  val onClose =
    createStateListener(WindowPropertyKeys.Closed, { closed }) { debugWindow("emit onClose", this) }

  fun isClosed() = state.closed

  /**
   * 关闭窗口
   * 一般来来说不要直接使用该核心接口，请使用 tryCloseOrHide 、 closeRoot 等业务接口进行替代
   */
  @LowLevelWindowAPI
  open suspend fun close(force: Boolean = false) {
    if (!force) {
      /// 如果有关闭提示，并且没有显示，那么就显示一下
      if (state.closeTip != null && !state.showCloseTip) {
        state.showCloseTip = true
        return
      }
    }
    state.closed = true
  }


  val isMainWindow get() = state.parent == null

  /**
   * 尝试关闭或者将窗口隐藏
   * 如果有父窗口，那么采用关闭
   * 如果自己就是主窗口，那么采用隐藏
   */
  @OptIn(LowLevelWindowAPI::class)
  suspend fun tryCloseOrHide(force: Boolean = false) {
    when {
      state.constants.microModule.value == null -> close(force)
      isMainWindow -> toggleVisible(false)
      else -> close(force)
    }
  }

  //#endregion

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
  open suspend fun setStyle(style: WindowStyle) {
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

  val navigation = WindowNavigation(state)


  open fun hideCloseTip() {
    state.showCloseTip = false
  }


  open fun toggleMenuPanel(show: Boolean? = null) {
    state.showMenuPanel = show ?: !state.showMenuPanel
  }

  fun hideMenuPanel() = toggleMenuPanel(false)

  fun showMenuPanel() = toggleMenuPanel(true)

  internal open suspend fun toggleAlwaysOnTop(onTop: Boolean? = null) {
    state.alwaysOnTop = onTop ?: !state.alwaysOnTop
  }

  open suspend fun toggleKeepBackground(keepBackground: Boolean? = null) {
    state.keepBackground = keepBackground ?: !state.keepBackground
  }


  open suspend fun toggleColorScheme(colorScheme: WindowColorScheme? = null) {
    state.colorScheme = colorScheme ?: state.colorScheme.next()
  }


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
  suspend fun removeModal(module: MicroModule.Runtime, modalId: String) =
    modalsLock.withLock("write") {
      val modal = state.modals[modalId] ?: return@withLock false
      modal.safeDestroy(module)
    }

  suspend fun updateModalCloseTip(modalId: String, closeTip: String? = null) =
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
  suspend fun closeModal(module: MicroModule.Runtime, modalId: String) =
    modalsLock.withLock("write") {
      val modal = state.modals[modalId] ?: return@withLock false
      modal.safeClose(module)
    }


  suspend fun show() = toggleVisible(true)
  suspend fun hide() = toggleVisible(false)
  suspend fun disableAlwaysOnTop() = toggleAlwaysOnTop(false)
  suspend fun enableAlwaysOnTop() = toggleAlwaysOnTop(true)
  suspend fun disableKeepBackground() = toggleKeepBackground(false)
  suspend fun enableKeepBackground() = toggleKeepBackground(true)
}

val LocalWindowController = compositionChainOf<WindowController>("WindowController")

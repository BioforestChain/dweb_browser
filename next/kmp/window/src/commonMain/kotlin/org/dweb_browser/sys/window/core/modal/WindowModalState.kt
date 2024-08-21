package org.dweb_browser.sys.window.core.modal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.core.windowAdapterManager

val debugModal = Debugger("modal")

@OptIn(LowLevelWindowAPI::class)
@Serializable
sealed class ModalState() {
  val modalId = randomUUID()
  abstract val callbackUrl: String?

  /**
   * 关闭提示
   * 如果非空，那么在用户尝试主动关闭模态窗口的时候，会弹出报警提示
   */
  var closeTip: String? = null

  /**
   * 是否只展示一次，之后自动销毁
   */
  val once = false

  @Transient
  protected val isOpenState = mutableStateOf(false)

  /**
   * 是否开启
   */
  var isOpen: Boolean = false
    private set
  val isClose get() = !isOpen

  var sessionId: Int = 1
    private set

  @LowLevelWindowAPI
  open fun open() {
    this.sessionId += 1;
    this.isOpen = true
    this.isOpenState.value = true
  }

  suspend fun safeOpen(): Boolean {
    if (this._isDestroyed || this.isOpen) {
      return false
    }
    open()
    parent.updateOpeningModal()
    return true
  }

  @LowLevelWindowAPI
  open fun close() {
    this.isOpen = false
    this.isOpenState.value = false
    this.showCloseTip.value = ""
  }

  @OptIn(LowLevelWindowAPI::class)
  suspend fun safeClose(mm: MicroModule.Runtime) = this.isOpen.trueAlso {
    close();
    /// 更新渲染中的modal
    parent.updateOpeningModal()
    // 发送 close 的信号
    sendCallback(mm, CloseModalCallback(sessionId))
    // 如果是一次性显示的，那么直接关闭它
    if (once) {
      mm.scopeLaunch(cancelable = false) {
        safeDestroy(mm)
      }
    }
  }

  @Transient
  private var _isDestroyed = false

  @Transient
  internal val afterDestroy = CompletableDeferred<Unit>()
  suspend fun safeDestroy(mm: MicroModule.Runtime): Boolean {
    if (this._isDestroyed) {
      return false
    }
    this._isDestroyed = true
    afterDestroy.complete(Unit)

    /// 从parent中移除
    parent.state.modals -= modalId
    /// 更新渲染中的modal
    parent.updateOpeningModal()
    /// 移除渲染器
    windowAdapterManager.renderProviders.remove(renderId)
    /// 触发回调
    sendCallback(mm, DestroyModalCallback(sessionId))

    return true
  }

  @Transient
  internal val showCloseTip = mutableStateOf("")

  internal val isShowCloseTip get() = showCloseTip.value.isNotEmpty()

  @Composable
  abstract fun Render()

  var renderId: String = ""
    private set

  @Transient
  internal lateinit var parent: WindowController
  internal fun initParent(win: WindowController) {
    parent = win
    win.state.modals += modalId to this
    renderId = win.id + "/" + modalId

    /// 主窗口关闭的时候，它也要跟着被销毁
    val mm =
      win.state.constants.microModule.value ?: throw Error("fail to get window's microModule")
    win.onClose {
      safeDestroy(mm)
    }
  }


  fun sendCallback(mm: MicroModule.Runtime, callbackData: ModalCallback) =
    callbackUrl?.also { url ->
      mm.scopeLaunch(cancelable = true) {
        mm.nativeFetch(
          PureClientRequest.fromJson(
            url, PureMethod.POST, body = callbackData
          )
        )
      }
    }

  /**
   * 关闭modal指令的发送器
   */
  @Transient
  internal val dismissFlow = MutableSharedFlow<Boolean>()

  @Composable
  protected fun TryShowCloseTip(onConfirmToClose: () -> Unit) {
    if (isShowCloseTip) {
      RenderCloseTipImpl(onConfirmToClose)
    }
  }

  private fun resetState() {
    close()
  }

  init {
    // 强制重制状态，确保反序列化出来的对象能正确工作
    resetState()
  }
}


@Serializable
sealed class ModalCallback {
  abstract val sessionId: Int
}

@Serializable
@SerialName("open")
data class OpenModalCallback(override val sessionId: Int) : ModalCallback();
@Serializable
@SerialName("close")
data class CloseModalCallback(override val sessionId: Int) : ModalCallback();
@Serializable
@SerialName("close-alert")
data class CloseAlertModalCallback(override val sessionId: Int, val confirm: Boolean) :
  ModalCallback();

@Serializable
@SerialName("destroy")
data class DestroyModalCallback(override val sessionId: Int) : ModalCallback();

enum class WindowModalState {
  INIT, OPENING, OPEN, CLOSING, CLOSE, DESTROYING, DESTROY,
}
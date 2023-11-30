package org.dweb_browser.sys.window.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.compose.LocalImageLoader
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.render.IconRender
import org.dweb_browser.sys.window.render.IdRender
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme
import org.dweb_browser.sys.window.render.LocalWindowPadding

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
  var closeTip
    get() = _closeTip
    set(value) {
      _closeTip = value
    }

  @SerialName("closeTip")
  private var _closeTip: String? = null


  /**
   * 是否只展示一次，之后自动销毁
   */
  val once = false

  @Transient
  protected val isOpenState = mutableStateOf(false)

  @SerialName("isOpen")
  private var _isOpen = false

  /**
   * 是否开启
   */
  val isOpen get() = _isOpen


  @SerialName("sessionId")
  private var _sessionId = 1;

  val sessionId get() = _sessionId

  @LowLevelWindowAPI
  open fun open() {
    this._sessionId += 1;
    this._isOpen = true
    this.isOpenState.value = true
  }

  suspend fun safeOpen(): Boolean {
    if (this._isDestroyed || this._isOpen) {
      return false
    }
    open()
    parent.openingModal.value = parent.getOpenModal()
    return true
  }

  @LowLevelWindowAPI
  open fun close() {
    this._isOpen = false
    this.isOpenState.value = false
    this.showCloseTip.value = ""
  }

  @OptIn(LowLevelWindowAPI::class)
  suspend fun safeClose(mm: MicroModule) = this._isOpen.trueAlso {
    close();
    /// 更新渲染中的modal
    parent.openingModal.value = parent.getOpenModal()
    // 发送 close 的信号
    sendCallback(mm, CloseModalCallback(sessionId))
    // 如果是一次性显示的，那么直接关闭它
    if (once) {
      mm.ioAsyncScope.launch {
        safeDestroy(mm)
      }
    }
  }

  @Transient
  private var _isDestroyed = false
  suspend fun safeDestroy(mm: MicroModule): Boolean {
    if (this._isDestroyed) {
      return false
    }
    this._isDestroyed = true

    /// 从parent中移除
    parent.state.modals -= modalId
    /// 更新渲染中的modal
    parent.openingModal.value = parent.getOpenModal()
    /// 移除渲染器
    windowAdapterManager.renderProviders.remove(renderId)
    /// 触发回调
    sendCallback(mm, DestroyModalCallback(sessionId))

    return true
  }

  @Transient
  protected val showCloseTip = mutableStateOf("")

  protected val isShowCloseTip get() = showCloseTip.value.isNotEmpty()

  @Composable
  abstract fun Render()

  @SerialName("renderId")
  private var _renderId: String = ""

  val renderId get() = _renderId

  @Transient
  protected lateinit var parent: WindowController
  internal fun setParent(win: WindowController) {
    parent = win
    win.state.modals += modalId to this
    _renderId = win.id + "/" + modalId
  }


  fun sendCallback(mm: MicroModule, callbackData: ModalCallback) = callbackUrl?.also { url ->
    mm.ioAsyncScope.launch {
      mm.nativeFetch(
        PureRequest.fromJson(
          url, IpcMethod.POST, body = callbackData
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
  protected fun RenderCloseTip(onConfirmToClose: () -> Unit) {
    if (isShowCloseTip) {
      AlertDialog(
        onDismissRequest = {
          showCloseTip.value = ""
        },
        title = { Text(text = "是否关闭抽屉面板") },
        text = { Text(text = showCloseTip.value) },
        confirmButton = {
          ElevatedButton(onClick = { showCloseTip.value = "" }) {
            Text("留下")
          }
        },
        dismissButton = {
          Button(onClick = {
            onConfirmToClose()
            // showCloseTip.value = "";
          }) {
            Text("关闭")
          }
        },
      )
    }
  }

  init {
    // 强制重制状态
    close()
  }
}

interface IAlertModalArgs {
  val title: String
  val message: String
  val iconUrl: String?
  val iconAlt: String?
  val confirmText: String?
  val dismissText: String?
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

@Serializable
@SerialName("alert")
data class AlertModal internal constructor(
  override val title: String,
  override val message: String,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val confirmText: String? = null,
  override val dismissText: String? = null,
  override val callbackUrl: String? = null,
) : ModalState(), IAlertModalArgs {
  companion object {
    fun WindowController.createAlertModal(
      title: String,
      message: String,
      iconUrl: String? = null,
      iconAlt: String? = null,
      confirmText: String? = null,
      dismissText: String? = null,
      callbackUrl: String? = null,
    ) = AlertModal(
      title, message, iconUrl, iconAlt, confirmText, dismissText, callbackUrl,
    ).also { it.setParent(this) }
  }

  @OptIn(FlowPreview::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val show by isOpenState

    fun onModalDismissRequest(isDismiss: Boolean) = mm.ioAsyncScope.launch {
      dismissFlow.emit(isDismiss)
    }

    /// 渲染关闭提示
    RenderCloseTip(onConfirmToClose = { onModalDismissRequest(true) })


    if (!show) {
      return
    }

    DisposableEffect(Unit) {
      // 发送 open 的信号
      sendCallback(mm, OpenModalCallback(sessionId))

      debugModal("DisposableEffect", " disposable")
      val job = dismissFlow.debounce(200).map {
        debugModal("dismissFlow", "close=$it")
        if (show && it) {
          safeClose(mm)
        }
      }.launchIn(mm.ioAsyncScope)
      onDispose {
        job.cancel()
        /// 如果被销毁，那么也要进行安全的关闭
        mm.ioAsyncScope.launch {
          safeClose(mm)
        }
      }
    }
    // alert的默认返回值
    var confirm = remember { false }

    AlertDialog(
      onDismissRequest = {
        onModalDismissRequest(true)
        sendCallback(mm, CloseAlertModalCallback(sessionId, confirm))
        if (once) {
          mm.ioAsyncScope.launch {
            parent.removeModal(mm, modalId)
          }
        }
      },
      confirmButton = {
        when (val text = confirmText) {
          null -> {}
          else -> ElevatedButton(onClick = { confirm = true; onModalDismissRequest(true) }) {
            Text(text)
          }
        }
      },
      dismissButton = {
        when (val text = dismissText) {
          null -> {}
          else -> Button(onClick = { onModalDismissRequest(true) }) {
            Text(text)
          }
        }
      },
      icon = {
        when (val url = iconUrl) {
          null -> Icon(imageVector = Icons.Default.WarningAmber, contentDescription = iconAlt)
          else -> {
            val imageLoader = LocalImageLoader.current

            /**
             * IconButtonTokens.IconSize
             */
            val iconSize = 24.dp
            imageLoader.Load(url, iconSize, iconSize).with(onError = {
              Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = iconAlt)
            }, onBusy = {
              CircularProgressIndicator(
                modifier = Modifier.width(iconSize),
                color = MaterialTheme.colorScheme.surfaceVariant,
                trackColor = MaterialTheme.colorScheme.secondary,
              )
            }) {
              Image(bitmap = it, contentDescription = iconAlt)
            }
          }
        }
      },
      title = {
        Text(text = title)
      },
      text = {
        Text(text = message)
      },
    )
  }
}

interface IBottomSheetModal {
  /// 这几个参数是未来使用FullScreenBottomSheets来实现，就是顶部有一条信息栏，将 bottom-sheets 拉上去的时候，融合信息栏，信息栏会变成 top-bar
  val title: String?
  val iconUrl: String?
  val iconAlt: String?
}

@Serializable
@SerialName("bottom-sheets")
class BottomSheetsModal private constructor(
  override val title: String? = null,
  override val iconUrl: String? = null,
  override val iconAlt: String? = null,
  override val callbackUrl: String? = null,
) : ModalState(), IBottomSheetModal {
  companion object {
    fun WindowController.createBottomSheetsModal(
      title: String? = null,
      iconUrl: String? = null,
      iconAlt: String? = null,
      callbackUrl: String? = null,
    ) = BottomSheetsModal(title, iconUrl, iconAlt, callbackUrl).also {
      it.setParent(this);
    }
  }

  /**
   * 是否已经展开，用于确保展开后才能进行 hidden 收回
   */
  @Transient
  private var hasExpanded = false

  @LowLevelWindowAPI
  override fun close() {
    super.close()
    this.hasExpanded = false
  }

  @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
  @Composable
  override fun Render() {
    val mm = LocalWindowMM.current
    val show by isOpenState

    fun onModalDismissRequest(isDismiss: Boolean) = mm.ioAsyncScope.launch {
      dismissFlow.emit(isDismiss)
    }

    /// 渲染关闭提示
    RenderCloseTip(onConfirmToClose = { onModalDismissRequest(true) })

    if (!show) {
      return
    }

    DisposableEffect(Unit) {
      // 发送 open 的信号
      sendCallback(mm, OpenModalCallback(sessionId))

      debugModal("DisposableEffect", " disposable")
      val job = dismissFlow.map {
        if (!it) {
          hasExpanded = true
        }
        it
      }.debounce(200).map {
        debugModal("dismissFlow", "close=$it hasExpanded=$hasExpanded")
        if (show && it && hasExpanded) {
          safeClose(mm)
        }
      }.launchIn(mm.ioAsyncScope)
      onDispose {
        job.cancel()
        /// 如果被销毁，那么也要进行安全的关闭
        mm.ioAsyncScope.launch {
          safeClose(mm)
        }
      }
    }

    val sheetState = rememberModalBottomSheetState(confirmValueChange = {
      debugModal("confirmValueChange", " $it")
      when (it) {
        SheetValue.Hidden -> closeTip.let { closeTip ->
          if (closeTip.isNullOrEmpty() || isShowCloseTip) {
            onModalDismissRequest(true)
            hasExpanded
          } else {
            showCloseTip.value = closeTip
            false
          }
        }

        SheetValue.Expanded -> {
          onModalDismissRequest(false);
          true
        }

        SheetValue.PartiallyExpanded -> {
          onModalDismissRequest(false);
          true
        }
      }
    });

    val density = LocalDensity.current
    val defaultWindowInsets = BottomSheetDefaults.windowInsets
    val modalWindowInsets = remember(defaultWindowInsets) {
      defaultWindowInsets.only(WindowInsetsSides.Top)
    }

    val win = parent;
    val winPadding = LocalWindowPadding.current
    val winTheme = LocalWindowControllerTheme.current
    val contentColor = winTheme.topContentColor
    /// TODO 等1.5.10稳定版放出，IOS才能使用这个组件
    ModalBottomSheet(sheetState = sheetState, dragHandle = {
      Box(
        modifier = Modifier
          .height(48.dp)
          .fillMaxSize()
          .padding(horizontal = 14.dp),
        Alignment.Center
      ) {
        BottomSheetDefaults.DragHandle()
        /// 应用图标
        Box(
          modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart
        ) {
          win.IconRender(
            modifier = Modifier.size(28.dp), primaryColor = contentColor
          )
        }
        /// 应用身份
        win.IdRender(
          Modifier
            .align(Alignment.BottomEnd)
            .height(22.dp)
            .padding(vertical = 2.dp)
            .alpha(0.4f),
          contentColor = contentColor
        )
      }
    }, windowInsets = modalWindowInsets, onDismissRequest = { onModalDismissRequest(true) }) {
      /// 显示内容
      BoxWithConstraints(
        Modifier.padding(
          start = winPadding.left.dp,
          end = winPadding.right.dp,
          bottom = (defaultWindowInsets.getBottom(density) / density.density).dp
        )
      ) {
        val windowRenderScope = remember(winPadding) {
          WindowRenderScope.fromDp(maxWidth, maxHeight, 1f)
        }
        windowAdapterManager.Renderer(
          renderId,
          windowRenderScope,
          Modifier.clip(winPadding.contentRounded.toRoundedCornerShape())
        )
      }

    }

  }
}


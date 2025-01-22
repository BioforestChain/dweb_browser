package org.dweb_browser.sys.window.core.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.constant.LowLevelWindowAPI
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.render.IconRender
import org.dweb_browser.sys.window.render.IdRender
import org.dweb_browser.sys.window.render.getVirtualNavigationBarHeight


//#region BottomSheet

interface IBottomSheetModal {
  /// 这几个参数是未来使用FullScreenBottomSheets来实现，就是顶部有一条信息栏，将 bottom-sheets 拉上去的时候，融合信息栏，信息栏会变成 top-bar
  val title: String?
  val iconUrl: String?
  val iconAlt: String?
}


@Serializable
@SerialName("bottom-sheets")
class BottomSheetsModalState private constructor(
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
    ) = BottomSheetsModalState(title, iconUrl, iconAlt, callbackUrl).also {
      it.initParent(this);
    }
  }

  /**
   * 是否已经展开，用于确保展开后才能进行 hidden 收回
   */
  @Transient
  internal var hasExpanded = false

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

    fun emitDismiss(isDismiss: Boolean) {
      mm.scopeLaunch(cancelable = true) {
        dismissFlow.emit(isDismiss)
      }
    }

    /**
     * 传入 EmitModalVisibilityState 指令，如果指令让状态发生了改变，那么返回 成功:true
     */
    val emitModalVisibilityChange: MutableState<(state: EmitModalVisibilityState) -> Boolean> =
      remember {
        mutableStateOf({ state ->
          when (state) {
            EmitModalVisibilityState.Open -> {
              emitDismiss(false)
              true
            }

            EmitModalVisibilityState.TryClose -> closeTip.let { closeTip ->
              if (closeTip.isNullOrEmpty() || isShowCloseTip) {
                emitDismiss(true)
                hasExpanded
              } else {
                showCloseTip.value = closeTip
                false
              }
            }

            EmitModalVisibilityState.ForceClose -> {
              emitDismiss(true);
              false
            }
          }
        })
      }

    /// 渲染关闭提示
    TryShowCloseTip(onConfirmToClose = { emitModalVisibilityChange.value(EmitModalVisibilityState.ForceClose) })

    if (!show) {
      return
    }

    DisposableEffect(Unit) {
      // 发送 open 的信号
      sendCallback(mm, OpenModalCallback(sessionId))

      debugModal("DisposableEffect", " disposable")
      val job = mm.scopeLaunch(cancelable = false) {
        dismissFlow.map { dismiss ->
          dismiss.falseAlso { hasExpanded = true }
        }.debounce(200).collect { dismiss ->
          debugModal("dismissFlow", "close=$dismiss hasExpanded=$hasExpanded")
          if (dismiss && show && hasExpanded) {
            safeClose(mm)
          }
        }
      }
      onDispose {
        job.cancel()
        /// 关闭动作只能被 dismiss 触发，不能因为Dispose触发，否则Activity重载时就会导致销毁
      }
    }
    RenderImpl(emitModalVisibilityChange.value)
  }
}

enum class EmitModalVisibilityState {
  Open, TryClose, ForceClose,
}

@Composable
internal expect fun BottomSheetsModalState.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean)

/**
 * 一个通用的 RenderImpl 实现，使用标准 Compose 来绘制这个 bottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomSheetsModalState.CommonRenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
  var isFirstView by remember { mutableStateOf(true) }
  var firstViewAction by remember { mutableStateOf({ }) }
  val uiScope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
    confirmValueChange = remember(emitModalVisibilityChange) {
      {
        /// 默认展开
        if (isFirstView) {
          firstViewAction()
        }

        debugModal("confirmValueChange", " $it")
        when (it) {
          SheetValue.Hidden -> isClose
          SheetValue.Expanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
          SheetValue.PartiallyExpanded -> emitModalVisibilityChange(EmitModalVisibilityState.Open)
        }
      }
    });
  if (isFirstView) {
    firstViewAction = {
      isFirstView = false
      uiScope.launch {
        sheetState.expand()
      }
    }
  }

  val density = LocalDensity.current
  val defaultWindowInsets = BottomSheetDefaults.windowInsets
  val modalWindowInsets = remember {
    WindowInsets(0, 0, 0, 0)
  }

  val winFrameStyle = LocalWindowFrameStyle.current

  // TODO 这个在Android/IOS上有BUG，会变成两倍大小，需要官方修复
  // https://issuetracker.google.com/issues/307160202
  val windowInsetTop = remember(defaultWindowInsets) {
    (defaultWindowInsets.getTop(density) / density.density / 2).dp
  }
  val windowInsetBottom = remember(defaultWindowInsets) {
    (defaultWindowInsets.getBottom(density) / density.density).dp
  }
  val virtualNavigationBarHeight = remember(defaultWindowInsets) {
    (getVirtualNavigationBarHeight() / density.density).dp
  }
  ModalBottomSheet(sheetState = sheetState,
    modifier = Modifier.padding(
      top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    ),
    dragHandle = {
      TitleBarWithOnClose({
        if (emitModalVisibilityChange(EmitModalVisibilityState.TryClose)) {
          uiScope.launch {
            sheetState.hide()
          }
        }
      }) {
        BottomSheetDefaults.DragHandle(Modifier.align(Alignment.TopCenter))
      }
    },
    contentWindowInsets = { modalWindowInsets },
    onDismissRequest = { emitModalVisibilityChange(EmitModalVisibilityState.TryClose) }) {
    /// 显示内容
    BoxWithConstraints(
      Modifier.padding(
        start = winFrameStyle.startWidth.dp,
        end = winFrameStyle.endWidth.dp,
        bottom = windowInsetBottom + windowInsetTop + virtualNavigationBarHeight
      )
    ) {
      val windowRenderScope = remember(winFrameStyle, maxWidth, maxHeight) {
        WindowContentRenderScope(maxWidth, maxHeight)
      }
      windowAdapterManager.Renderer(
        renderId, windowRenderScope, Modifier.clip(winFrameStyle.contentRounded.roundedCornerShape)
      )
    }
  }

}


@Composable
fun BottomSheetsModalState.TitleBarWithCustomCloseButton(
  closeBottom: @Composable (Modifier) -> Unit, content: @Composable BoxScope.() -> Unit,
) {
  val winTheme = LocalWindowControllerTheme.current
  val contentColor = winTheme.topContentColor
  val win = parent
  val size = 48.dp
  Box(
    modifier = Modifier.height(size).fillMaxSize(), Alignment.Center
  ) {
    /// 应用图标
    Box(
      modifier = Modifier.align(Alignment.CenterStart).size(size)
        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      win.IconRender(
        modifier = Modifier.fillMaxSize(), primaryColor = contentColor
      )
    }
    /// 应用身份 与 关闭按钮
    Row(
      Modifier.align(Alignment.BottomEnd),
      verticalAlignment = Alignment.Bottom,
    ) {
      win.IdRender(
        Modifier.height(22.dp).padding(vertical = 2.dp).alpha(0.4f), contentColor = contentColor
      )
      closeBottom(Modifier.size(size))
    }
    content()
  }
}

@Composable
fun BottomSheetsModalState.TitleBarWithOnClose(
  onClose: () -> Unit, content: @Composable BoxScope.() -> Unit,
) {
  TitleBarWithCustomCloseButton(
    { modifier ->
      val winTheme = LocalWindowControllerTheme.current
      Box(
        modifier.padding(13.dp), contentAlignment = Alignment.Center
      ) {
        IconButton(
          {
            onClose()
          }, colors = IconButtonDefaults.iconButtonColors(
            contentColor = winTheme.topBackgroundColor, containerColor = winTheme.topContentColor
          )
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close Bottom Sheet",
            modifier = Modifier.padding(3.dp)
          )
        }
      }
    }, content
  )
}
//#endregion

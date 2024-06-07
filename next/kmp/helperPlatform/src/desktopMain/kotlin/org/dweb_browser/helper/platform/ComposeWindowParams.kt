package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.withScope
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.awt.event.WindowListener

class ComposeWindowParams(
  private val pvc: PureViewController,
  private val innerContent: @Composable FrameWindowScope.() -> Unit
) {
  companion object {
    var defaultIcon by mutableStateOf<Painter?>(null)
  }

  val content: @Composable FrameWindowScope.() -> Unit = {
    window.minimumSize = Dimension(200, 200) // 设置窗口最小值，避免过小引起的渲染异常
    frameWindowScope = this
    innerContent()
  }

  class CacheWindowState(
    override var isMinimized: Boolean,
    override var placement: WindowPlacement,
    override var position: WindowPosition,
    override var size: DpSize
  ) : WindowState

  internal var state: WindowState = CacheWindowState(
    isMinimized = false, placement = WindowPlacement.Floating,
    position = WindowPosition.PlatformDefault,
    // 目前 BrowserHorizontalPager 在desktop时，默认显示三个，每个220.dp, 间隔5.dp, 左右各10.dp,总共700.dp。
    // 然后还有三个按钮各 40.dp，所以宽度总共 700 + 120 = 820.dp，但是window显示有问题，需要使用835.dp
    size = if (PureViewController.isWindows) DpSize(835.dp, 600.dp) else DpSize(820.dp, 600.dp)
  )

  /**是否最小化*/
  var isMinimized
    get() = state.isMinimized
    set(value) {
      state.isMinimized = value
    }

  /**是否全屏*/
  val isFullscreen
    get() = placement == WindowPlacement.Fullscreen

  /**是否最大化*/
  val isMaximized
    get() = placement == WindowPlacement.Maximized

  /**控制窗口状态*/
  var placement: WindowPlacement
    get() = state.placement
    set(value) {
      state.placement = value
    }

  // 控制窗口位置
  var position
    get() = state.position
    set(value) {
      state.position = value
    }
  var size
    get() = state.size
    set(value) {
      state.size = value
    }

  var frameWindowScope by mutableStateOf<FrameWindowScope?>(null)
    private set
  var onCloseRequest by mutableStateOf<() -> Unit>({})
  var visible by mutableStateOf<Boolean>(true)
  var title by mutableStateOf<String>("Untitled")
  var icon by mutableStateOf<Painter?>(defaultIcon)
  val defaultIcon get() = ComposeWindowParams.defaultIcon
  var undecorated by mutableStateOf<Boolean>(false)
  var transparent by mutableStateOf<Boolean>(false)
  var resizable by mutableStateOf<Boolean>(true)
  var enabled by mutableStateOf<Boolean>(true)
  var focusable by mutableStateOf<Boolean>(true)
  var alwaysOnTop by mutableStateOf<Boolean>(false)
  var onPreviewKeyEvent by mutableStateOf<(KeyEvent) -> Boolean>({ false })
  var onKeyEvent by mutableStateOf<(KeyEvent) -> Boolean>({ false })

  val isOpened get() = PureViewController.windowRenders.contains(this)
  private val openCloseLock = Mutex()

  suspend fun openWindow() = openCloseLock.withLock {
    if (isOpened) {
      return@withLock
    }
    // 触发窗口创建事件
    withScope(pvc.lifecycleScope) {
      pvc.createSignal.emit(pvc.createParams)
    }
    // 添加渲染参数
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.add(this@ComposeWindowParams)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun closeWindow() = openCloseLock.withLock {
    if (!isOpened) {
      return@withLock
    }
    pvc.composeWindowStateFlow.resetReplayCache()
    withScope(pvc.lifecycleScope) {
      pvc.destroySignal.emitAndClear()
    }
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.remove(this@ComposeWindowParams)
    }
  }

  private fun <T> MutableSharedFlow<T>.launchEmit(value: T) {
    pvc.lifecycleScope.launch { emit(value) }
  }

  val windowEvents = WindowEvents()

  // 统一管理窗口事件
  inner class WindowEvents {
    private val _windowOpened = MutableSharedFlow<WindowEvent>()
    val windowOpened = _windowOpened.asSharedFlow()
    private val _windowClosing = MutableSharedFlow<WindowEvent>()
    val windowClosing = _windowClosing.asSharedFlow()
    private val _windowClosed = MutableSharedFlow<WindowEvent>()
    val windowClosed = _windowClosed.asSharedFlow()
    private val _windowIconified = MutableSharedFlow<WindowEvent>()
    val windowIconified = _windowIconified.asSharedFlow()
    private val _windowDeiconified = MutableSharedFlow<WindowEvent>()
    val windowDeiconified = _windowDeiconified.asSharedFlow()
    private val _windowActivated = MutableSharedFlow<WindowEvent>()
    val windowActivated = _windowActivated.asSharedFlow()
    private val _windowDeactivated = MutableSharedFlow<WindowEvent>()
    val windowDeactivated = _windowDeactivated.asSharedFlow()

    init {
      pvc.lifecycleScope.launch {
        pvc.composeWindowStateFlow.collect { composeWindow ->
          composeWindow.addWindowListener(object : WindowListener {
            override fun windowOpened(event: WindowEvent) {
              _windowOpened.launchEmit(event)
            }

            override fun windowClosing(event: WindowEvent) {
              _windowClosing.launchEmit(event)
            }

            override fun windowClosed(event: WindowEvent) {
              _windowClosed.launchEmit(event)
            }

            // 窗口图标化
            override fun windowIconified(event: WindowEvent) {
              _windowIconified.launchEmit(event)
            }

            // 取消窗口图标化
            override fun windowDeiconified(event: WindowEvent) {
              _windowDeiconified.launchEmit(event)
            }

            // 窗口激活
            override fun windowActivated(event: WindowEvent) {
              _windowActivated.launchEmit(event)
            }

            // 窗口停用事件，比如缩小到dock
            override fun windowDeactivated(event: WindowEvent) {
              _windowDeactivated.launchEmit(event)
            }
          })
        }
      }
    }
  }

  val componentEvents = ComponentEvents()

  inner class ComponentEvents {
    private val _componentResized = MutableSharedFlow<ComponentEvent>()
    val componentResized = _componentResized.asSharedFlow()
    private val _componentMoved = MutableSharedFlow<ComponentEvent>()
    val componentMoved = _componentMoved.asSharedFlow()
    private val _componentShown = MutableSharedFlow<ComponentEvent>()
    val componentShown = _componentShown.asSharedFlow()
    private val _componentHidden = MutableSharedFlow<ComponentEvent>()
    val componentHidden = _componentHidden.asSharedFlow()

    init {
      pvc.lifecycleScope.launch {
        pvc.composeWindowStateFlow.collect { composeWindow ->
          // 监听原生窗口一些状态，用来通过副作用给控制器同步状态
          composeWindow.addComponentListener(object : ComponentListener {
            override fun componentResized(event: ComponentEvent) {
              _componentResized.launchEmit(event)
            }

            override fun componentMoved(event: ComponentEvent) {
              _componentMoved.launchEmit(event)
            }

            override fun componentShown(event: ComponentEvent) {
              _componentShown.launchEmit(event)
            }

            override fun componentHidden(event: ComponentEvent) {
              _componentHidden.launchEmit(event)
            }
          })
        }
      }
    }
  }

  val windowFocusStateFlow = WindowFocusStateFlow()

  // 窗口失去焦点获得焦点状态监听
  inner class WindowFocusStateFlow(
    private val sharedFlow: MutableStateFlow<AwtWindowFocusState> = MutableStateFlow(
      AwtWindowFocusState.Lost
    )
  ) : StateFlow<AwtWindowFocusState> by sharedFlow.asStateFlow() {
    init {
      pvc.lifecycleScope.launch {
        pvc.composeWindowStateFlow.collect { composeWindow ->
          composeWindow.addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(event: WindowEvent) {
              sharedFlow.launchEmit(AwtWindowFocusState.Gained)
            }

            override fun windowLostFocus(event: WindowEvent) {
              sharedFlow.launchEmit(AwtWindowFocusState.Lost)
            }
          })
        }
      }
    }
  }

  // 窗口是否聚焦状态
  enum class AwtWindowFocusState {
    Gained, Lost,
  }

  val mouseMotionEvents = MouseMotionEvents()

  // 鼠标运动触发的副作用事件
  inner class MouseMotionEvents {
    private val _mouseDragged = MutableSharedFlow<MouseEvent>()
    val mouseDragged = _mouseDragged.asSharedFlow()
    private val _mouseMoved = MutableSharedFlow<MouseEvent>()
    val mouseMoved = _mouseMoved.asSharedFlow()

    init {
      pvc.lifecycleScope.launch {
        pvc.composeWindowStateFlow.collect { composeWindow ->
          composeWindow.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(event: MouseEvent) {
              _mouseDragged.launchEmit(event)
            }

            override fun mouseMoved(event: MouseEvent) {
              _mouseMoved.launchEmit(event)
            }
          })
        }
      }
    }
  }

  val windowFrameSharedFlow = WindowStateSharedFlow()

  inner class WindowStateSharedFlow(
    private val sharedFlow: MutableSharedFlow<AwtWindowFrameState> = MutableSharedFlow(replay = 1)
  ) : SharedFlow<AwtWindowFrameState> by sharedFlow.asSharedFlow() {
    init {
      pvc.lifecycleScope.launch {
        pvc.composeWindowStateFlow.collect { composeWindow ->
          composeWindow.addWindowStateListener { event ->
            sharedFlow.launchEmit(AwtWindowFrameState.ALL_VALUES[event.newState]!!)
          }
        }
      }
    }
  }

  /**
   * 参考 java.awt.Frame;
   */
  enum class AwtWindowFrameState(val value: Int) {
    /**
     * Frame.NORMAL：值为0，表示窗口处于正常状态（没有最大化或最小化）。
     */
    NORMAL(0),

    /**
     * Frame.ICONIFIED：值为1，表示窗口当前被最小化了。
     */
    ICONIFIED(1),

    /**
     * Frame.MAXIMIZED_HORIZ：值为2，表示窗口在水平方向上最大化了。
     */
    MAXIMIZED_HORIZ(2),

    /**
     * Frame.MAXIMIZED_VERT：值为4，表示窗口在垂直方向上最大化了。
     */
    MAXIMIZED_VERT(4),

    /**
     * Frame.MAXIMIZED_BOTH：值为6，表示窗口在水平方向和垂直方向都最大化了。
     */
    MAXIMIZED_BOTH(6),
    ;

    companion object {
      val ALL_VALUES = entries.associateBy { it.value }
    }
  }
//  val addWindowStateListener
}


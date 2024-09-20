package org.dweb_browser.helper.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.ImageCapture
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.platform.desktop.os.WindowsRegistry
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit


class PureViewController(
  var createParams: PureViewCreateParams = PureViewCreateParams(mapOf()),
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  companion object {

    val osName by lazy { System.getProperty("os.name").lowercase() }
    val isWindows by lazy { osName.contains("win") }
    val isMacOS by lazy { osName.contains("mac") }
    val isLinux by lazy { osName.contains("nix") || osName.contains("nux") }

    internal val windowRenders = mutableStateListOf<ComposeWindowParams>()

    var density = 1f
      private set
    lateinit var uiScope: CoroutineScope
      private set

    private val prepared = CompletableDeferred<Unit>()
    suspend fun awaitPrepared() {
      prepared.await()
    }

    // dweb_browser桌面窗口退出
    var exitDesktop: suspend () -> Unit = {}
      private set

    val contents = mutableStateMapOf<String, @Composable ApplicationScope.() -> Unit>()

    @Composable
    private fun ApplicationScope.Contents() {
      for (content in contents.values) {
        content()
      }
    }

    @Composable
    private fun ApplicationScope.Windows() {
      for (winRender in windowRenders) {
        key(winRender) {
          val state = rememberWindowState()
          // state要独立存储，否则 position、size 会导致这里重复重组
          if (winRender.state != state) {
            val oldState = winRender.state
            state.apply {
              placement = oldState.placement
              isMinimized = oldState.isMinimized
              position = oldState.position
              size = oldState.size
            }
            winRender.state = state
          }
          // 桌面端创建窗口并且绑定一大堆事件
          Window(
            onCloseRequest = winRender.onCloseRequest,
            state = state,
            visible = winRender.visible,
            title = winRender.title,
            icon = winRender.icon ?: winRender.defaultIcon,
            undecorated = winRender.undecorated,
            transparent = winRender.transparent,
            resizable = winRender.resizable,
            enabled = winRender.enabled,
            focusable = winRender.focusable,
            alwaysOnTop = winRender.alwaysOnTop,
            onPreviewKeyEvent = winRender.onPreviewKeyEvent,
            onKeyEvent = winRender.onKeyEvent,
            content = winRender.content,
          )
        }
      }
    }

    @Composable
    private fun ApplicationScope.Prepare() {
      // 初始化退出事件
      exitDesktop = {
        exitApplication()
//        exitProcess(0)
      }
      // 目前除了windows，其它平台（android、ios、macos）都能让背景透明地进行渲染
      envSwitch.init(ENV_SWITCH_KEY.DWEBVIEW_ENABLE_TRANSPARENT_BACKGROUND) { "${!isWindows}" }
      density = LocalDensity.current.density
      uiScope = rememberCoroutineScope()
      // windows dweb deeplink写入注册表
      if (isWindows) {
        WindowsRegistry.ensureWindowsRegistry("dweb")
      }

      prepared.complete(Unit)
    }

    // 控制截屏窗口的显示
    private var screenBitmap by mutableStateOf<ImageBitmap?>(null)
    private var captureDeferred = CompletableDeferred<ImageBitmap>()

    @Composable
    private fun ApplicationScope.CaptureWindow() {
      val screenSize = Toolkit.getDefaultToolkit().screenSize

      screenBitmap?.let { imageBitmap ->
        Window(
          onCloseRequest = ::exitApplication,
          state = rememberWindowState(
            placement = WindowPlacement.Fullscreen,
            size = DpSize(screenSize.width.dp, screenSize.height.dp)
          ),
          undecorated = true,
          transparent = true,
          resizable = false,
          alwaysOnTop = true,
          onKeyEvent = { keyEvent ->
            if (keyEvent.key == androidx.compose.ui.input.key.Key.Escape) {
              screenBitmap = null
              true
            } else false
          }
        ) {
          val device = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
          window.cursor = Cursor(Cursor.CROSSHAIR_CURSOR) // 设置鼠标样式为 十字星
          if (!isWindows) {
            device.fullScreenWindow = window
          }
          DisposableEffect(Unit) {
            onDispose { device.fullScreenWindow = null }
          }

          Box {
            // 显示原始截图
            Image(bitmap = imageBitmap, contentDescription = "Screenshot")
            // 显示十字线以及截屏矩形框
            // ScreenCapture(imageBitmap) { imageBitmap ->
            ImageCapture(imageBitmap) { imageBitmap ->
              // 获取截图信息，并调用SmartScan进行解析
              captureDeferred.complete(imageBitmap)
              screenBitmap = null
            }
          }
        }
      }
    }

    suspend fun startApplication(extContent: @Composable ApplicationScope.() -> Unit = {}) =
      awaitApplication {
        Prepare()
        Contents()
        Windows()
        extContent()
        CaptureWindow()
      }

    /**
     * 截屏操作
     */
    suspend fun awaitScreenCapture(): ImageBitmap {
      captureDeferred = CompletableDeferred()
      val screenSize = Toolkit.getDefaultToolkit().screenSize
      val rectangle = Rectangle(screenSize)
      screenBitmap = Robot().createScreenCapture(rectangle).toComposeImageBitmap()
      return captureDeferred.await()
    }
  }

  override var lifecycleScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
    private set

  private val initDeferred = CompletableDeferred<Unit>()
  suspend fun waitInit() = initDeferred.await()

  internal val createSignal = Signal<IPureViewCreateParams>()
  override val onCreate = createSignal.toListener()

  internal val stopSignal = SimpleSignal()
  override val onStop = stopSignal.toListener()

  internal val startSignal = SimpleSignal() // TODO 没有调用实现
  override val onStart = startSignal.toListener()

  internal val resumeSignal = SimpleSignal()
  override val onResume = resumeSignal.toListener()

  internal val pauseSignal = SimpleSignal() // TODO 没有调用实现
  override val onPause = pauseSignal.toListener()

  internal val destroySignal = SimpleSignal()
  override val onDestroy = destroySignal.toListener()

  private val touchSignal = Signal<TouchEvent>()
  suspend fun emitTouchSignal(ev: TouchEvent) {
    touchSignal.emit(ev)
  }

  override val onTouch = touchSignal.toListener()

  internal val pureViewBox by lazy {
    PureViewBox(this)
  }

  internal val composeWindowStateFlow = MutableSharedFlow<ComposeWindow>(1)
  suspend fun awaitComposeWindow() = composeWindowStateFlow.first()

  @Suppress("UNCHECKED_CAST")
  @Composable
  fun composeWindowAsState() =
    composeWindowStateFlow.collectAsState(getComposeWindowOrNull()) as State<ComposeWindow>

  fun getComposeWindowOrNull() = composeWindowStateFlow.replayCache.firstOrNull()

  val composeWindowParams by lazy {
    ComposeWindowParams(this) {
      composeWindowStateFlow.tryEmit(window)
      LocalCompositionChain.current.Provider(
        LocalPureViewController provides this@PureViewController,
        LocalPureViewBox provides pureViewBox,
      ) {
        for (content in contents) {
          content()
        }
      }
    }.apply {
      onCloseRequest = {
        lifecycleScope.launch { closeWindow() }
      }
    }
  }

  private val contents = mutableStateListOf<@Composable () -> Unit>();
  override fun getContents(): MutableList<@Composable () -> Unit> {
    return contents
  }
}

class PureViewCreateParams(private val params: Map<String, Any?>) : Map<String, Any?> by params,
  IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
  override fun getBoolean(key: String): Boolean? = get(key).let { require(it is Boolean?);it }
};


fun IPureViewController.asDesktop(): PureViewController {
  require(this is PureViewController)
  return this
}

actual val IPureViewController.Companion.platform
  get() = PureViewControllerPlatform.DesktopJvm
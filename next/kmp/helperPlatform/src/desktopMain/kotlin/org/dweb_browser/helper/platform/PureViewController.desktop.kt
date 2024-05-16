package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import dweb_browser_kmp.helperplatform.generated.resources.Res
import dweb_browser_kmp.helperplatform.generated.resources.tray_dweb_browser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.platform.desktop.os.WindowsRegistry
import org.jetbrains.compose.resources.painterResource
import kotlin.system.exitProcess


class PureViewController(
  var createParams: PureViewCreateParams = PureViewCreateParams(mapOf()),
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  companion object {

    private val osName by lazy { System.getProperty("os.name").lowercase() }
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

    suspend fun startApplication() = awaitApplication {
      // 退出应用
      suspend fun exitApp() {
        LocalViewHookFlow.emit(TrayEvent.Exit)
        exitApplication()
        exitProcess(0)
      }
      // 初始化退出事件
      exitDesktop = {
        exitApp()
      }
      // 目前除了windows，其它平台（android、ios、macos）都能让背景透明地进行渲染
      envSwitch.init("dwebview-enable-transparent-background") { "${!isWindows}" }

      Tray(icon = painterResource(Res.drawable.tray_dweb_browser), menu = {
        Item("Js Process", enabled = LocalViewHookJsProcess.isUse) {
          runBlocking {
            LocalViewHookFlow.emit(TrayEvent.JsProcess)
          }
        }
        Item("Exit App") {
          runBlocking {
            exitApp()
          }
        }
      })

      // windows dweb deeplink写入注册表
      if (isWindows) {
        WindowsRegistry.ensureWindowsRegistry("dweb")
      }

      density = LocalDensity.current.density
      uiScope = rememberCoroutineScope()
      prepared.complete(Unit)
      for (winRender in windowRenders) {
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
          icon = winRender.icon,
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

  init {
//    nativeViewController.onInitSignal.listen {
//      if (it == vcId) {
//        offListener()
//        isAdded = true
//        initDeferred.complete(Unit)
//      }
//    }
//    nativeViewController.onDestroySignal.listen {
//      if (it == vcId) {
//        destroySignal.emit()
//        isAdded = false
//        lifecycleScope.cancel(CancellationException("viewController destroyed"))
//        lifecycleScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
//      }
//    }
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
  override fun getContents(): MutableList<() -> Unit> {
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
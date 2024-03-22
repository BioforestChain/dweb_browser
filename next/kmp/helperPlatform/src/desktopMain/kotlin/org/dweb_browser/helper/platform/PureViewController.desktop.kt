package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.withScope


class ComposeWindowParams(
  private val pvc: PureViewController, val content: @Composable FrameWindowScope.() -> Unit
) {
  class CacheWindowState(
    override var isMinimized: Boolean,
    override var placement: WindowPlacement,
    override var position: WindowPosition,
    override var size: DpSize
  ) : WindowState

  internal var state: WindowState = CacheWindowState(
    isMinimized = false, placement = WindowPlacement.Floating,
    position = WindowPosition.PlatformDefault,
    size = DpSize(800.dp, 600.dp),
  )
  var isMinimized
    get() = state.isMinimized
    set(value) {
      state.isMinimized = value
    }
  var placement
    get() = state.placement
    set(value) {
      state.placement = value
    }
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

  var onCloseRequest by mutableStateOf<() -> Unit>({})
  var visible by mutableStateOf<Boolean>(true)
  var title by mutableStateOf<String>("Untitled")
  var icon by mutableStateOf<Painter?>(null)
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
    withScope(pvc.lifecycleScope) {
      pvc.createSignal.emit(pvc.createParams)
    }
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
}

class PureViewController(
  var createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  var isAdded = false
    private set

  companion object {
    internal val windowRenders = mutableStateListOf<ComposeWindowParams>()

    var density = 1f
      private set
    lateinit var uiScope: CoroutineScope
      private set

    private val prepared = CompletableDeferred<Unit>()
    suspend fun awaitPrepared() {
      prepared.await()
    }

    suspend fun startApplication() = awaitApplication {
      // https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/whats-new/whats-new-compose-1-6-0.md#desktop-experimental
      System.setProperty("compose.layers.type", "COMPONENT")
      // https://github.com/JetBrains/compose-multiplatform-core/pull/915
      System.setProperty("compose.interop.blending", "true")
      System.setProperty("compose.swing.render.on.graphics", "true")

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
  @Composable fun composeWindowAsState() = composeWindowStateFlow.collectAsState(getComposeWindowOrNull()) as State<ComposeWindow>

  fun getComposeWindowOrNull() = composeWindowStateFlow.replayCache.firstOrNull()

  @OptIn(ExperimentalCoroutinesApi::class)
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
//      transparent = true,
//      undecorated = true,
//      placement = WindowPlacement.Maximized,
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
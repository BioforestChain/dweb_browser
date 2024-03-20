package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
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
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.withScope


class ComposeWindowParams(
  private val pvc: PureViewController,
  val onCloseRequest: () -> Unit,
  val placement: WindowPlacement = WindowPlacement.Floating,
  val isMinimized: Boolean = false,
  val position: WindowPosition = WindowPosition.PlatformDefault,
  val size: DpSize = DpSize(800.dp, 600.dp),
  val visible: Boolean = true,
  val title: String = "Untitled",
  val icon: Painter? = null,
  val undecorated: Boolean = false,
  val transparent: Boolean = false,
  val resizable: Boolean = true,
  val enabled: Boolean = true,
  val focusable: Boolean = true,
  val alwaysOnTop: Boolean = false,
  val onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
  val onKeyEvent: (KeyEvent) -> Boolean = { false },
  val content: @Composable FrameWindowScope.() -> Unit
) {
  suspend fun openWindow() {
    withScope(pvc.lifecycleScope) {
      pvc.createSignal.emit(pvc.createParams)
    }
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.add(this@ComposeWindowParams)
    }
  }

  suspend fun closeWindow() {
    withScope(pvc.lifecycleScope) {
      pvc.destroySignal.emitAndClear()
    }
    withScope(PureViewController.uiScope) {
      PureViewController.windowRenders.remove(this@ComposeWindowParams)
    }
  }
}

class PureViewController(
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
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
      density = LocalDensity.current.density
      uiScope = rememberCoroutineScope()
      prepared.complete(Unit)
      for (winRender in windowRenders) {
        Window(
          onCloseRequest = winRender.onCloseRequest,
          state = rememberWindowState(
            placement = winRender.placement,
            isMinimized = winRender.isMinimized,
            position = winRender.position,
            size = winRender.size
          ),
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

  private val composeWindowStateFlow = MutableSharedFlow<ComposeWindow>(1)
  suspend fun awaitComposeWindow() = composeWindowStateFlow.first()

  fun getComposeWindowOrNull() = composeWindowStateFlow.replayCache.firstOrNull()

  val composeWindow by lazy {
    var onCloseRequest = {}
    ComposeWindowParams(pvc = this, onCloseRequest = {
      onCloseRequest()
    }) {
      composeWindowStateFlow.tryEmit(window)
      LocalCompositionChain.current.Provider(
        LocalPureViewController provides this@PureViewController,
        LocalPureViewBox provides pureViewBox,
      ) {
        for (content in contents) {
          content()
        }
      }
    }.also {
      onCloseRequest = {
        lifecycleScope.launch { it.closeWindow() }
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

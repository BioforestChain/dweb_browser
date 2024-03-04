package org.dweb_browser.helper.platform

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.yield
import org.dweb_browser.helper.Once
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.mainAsyncExceptionHandler


private var vcIdAcc by SafeInt(0);

class PureViewController(
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  var isAdded = false
    private set

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

  private val createSignal = Signal<IPureViewCreateParams>()
  override val onCreate = createSignal.toListener()

  private val stopSignal = SimpleSignal()
  override val onStop = stopSignal.toListener()

  private val startSignal = SimpleSignal() // TODO 没有调用实现
  override val onStart = startSignal.toListener()

  private val resumeSignal = SimpleSignal()
  override val onResume = resumeSignal.toListener()

  private val pauseSignal = SimpleSignal() // TODO 没有调用实现
  override val onPause = pauseSignal.toListener()

  private val destroySignal = SimpleSignal()
  override val onDestroy = destroySignal.toListener()

  private val touchSignal = Signal<TouchEvent>()
  suspend fun emitTouchSignal(ev: TouchEvent) {
    touchSignal.emit(ev)
  }

  override val onTouch = touchSignal.toListener()


  class ApplicationApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) = Unit
    override fun up() = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun clear() = Unit
    override fun onEndChanges() = Unit
  }

  val applier = ApplicationApplier()

  object YieldFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
      onFrame: (frameTimeNanos: Long) -> R
    ): R {
      // We call `yield` to avoid blocking UI thread. If we don't call this then application
      // can be frozen for the user in some cases as it will not receive any input events.
      //
      // Swing dispatcher will process all pending events and resume after `yield`.
      yield()
      return onFrame(System.nanoTime())
    }
  }

  private val composePanel by lazy {
    ComposePanel()
  }
  internal val pureViewBox by lazy {
    PureViewBox(composePanel, this)
  }

  private var _density = 1f
  val density get() = _density

  @OptIn(ExperimentalComposeUiApi::class)
  val getJPanel = Once {
    composePanel.also { panel ->
      panel.setContent {
        _density = LocalDensity.current.density
        LocalCompositionChain.current.Provider(
          LocalPureViewController provides this,
          LocalPureViewBox provides pureViewBox,
        ) {
          for (content in contents) {
            content()
          }
        }
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

package org.dweb_browser.helper.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.zIndex
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.BgPlaceholderView
import platform.UIKit.UIView

private var vcIdAcc by SafeInt(0);

class PureViewController(
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  var prop = DwebUIViewControllerProperty(vcIdAcc++, -1, false)
  val vcId get() = prop.vcId

  var isAdded = false
    private set

  init {
    nativeViewController.onInitSignal.listen {
      if (it == vcId) {
        offListener()
        isAdded = true
        initDeferred.complete(Unit)
      }
    }
    nativeViewController.onDestroySignal.listen {
      if (it == vcId) {
        destroySignal.emit()
        isAdded = false
        lifecycleScope.cancel(CancellationException("viewController destroyed"))
        lifecycleScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
      }
    }
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

  private val scope = nativeViewController.scope

  @OptIn(ExperimentalForeignApi::class)
  val getUiViewController = SuspendOnce {
    withMainContext {
      val backgroundView = mutableStateOf<UIView?>(null)
      val bgPlaceholderView = BgPlaceholderView()
      bgPlaceholderView.setCallback { bgView ->
        backgroundView.value = bgView
        bgView?.apply {
          bgView.setHidden(true)
        }
      }
      ComposeUIViewController({
        delegate = object : ComposeUIViewControllerDelegate {
          // 视图被加载后立即调用
          override fun viewDidLoad() {
            scope.launch { createSignal.emit(createParams) }
          }
          //当视图控制器的视图即将被添加到视图层次结构中时触发
          override fun viewWillAppear(animated: Boolean) {
            scope.launch { resumeSignal.emit() }
          }
          // 视图控制器的视图已经被添加到视图层次结构后调用
          override fun viewDidAppear(animated: Boolean) {
            backgroundView.value?.also { bgView ->
              bgView.superview?.sendSubviewToBack(bgView)
            }
            scope.launch { stopSignal.emit() }
          }
          // 在视图即将从视图层次结构中移除时调用
          override fun viewWillDisappear(animated: Boolean) {
            println("QWQ viewWillDisappear animated=$animated")
          }
          // 视图已经从视图层次结构中移除后会调用此函数
          override fun viewDidDisappear(animated: Boolean) {
            println("QWQ viewDidDisappear animated=$animated")
          }
        }
      }) {
        UIKitView(
          factory = { bgPlaceholderView },
          Modifier.fillMaxSize().zIndex(0f),
          interactive = true
        )

        LocalCompositionChain.current.Provider(
          LocalPureViewController provides this,
          LocalPureViewBox provides PureViewBox(LocalUIViewController.current),
          LocalUIKitBackgroundView provides backgroundView.value,
        ) {
//      DwebBrowserAppTheme {
          for (content in contents) {
            content()
          }
//      }
        }
      }
    }
  }

  private val contents = mutableStateListOf<@Composable () -> Unit>();
  override fun getContents(): MutableList<() -> Unit> {
    return contents
  }
}

val LocalUIKitBackgroundView = compositionChainOf<UIView?>("UIKitBackgroundView") { null }

class PureViewCreateParams(private val params: Map<String, Any?>) : Map<String, Any?> by params,
  IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
  override fun getBoolean(key: String): Boolean? = get(key).let { require(it is Boolean?);it }
};

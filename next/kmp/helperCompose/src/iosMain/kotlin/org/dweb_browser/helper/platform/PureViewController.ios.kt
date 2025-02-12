package org.dweb_browser.helper.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.zIndex
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.BgPlaceholderView
import platform.Foundation.NSProcessInfo
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView

actual val IPureViewController.Companion.platform
  get() = PureViewControllerPlatform.IOS

private var vcIdAcc by SafeInt(0);

class PureViewController(
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf()),
  fullscreen: Boolean? = null,
) : IPureViewController {
  constructor(params: Map<String, Any?>, fullscreen: Boolean? = null) : this(
    PureViewCreateParams(
      params
    ),
    fullscreen,
  )

  var prop = DwebUIViewControllerProperty(
    vcId = vcIdAcc++,
    zIndex = -1,
    visible = false,
    fullscreen = fullscreen ?: true,
  )
  val vcId get() = prop.vcId

  var isAdded = false
    private set

  private val boundsFlow = MutableSharedFlow<PureRect>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  private val viewAppearFlow = MutableStateFlow(false)

  suspend fun setBounds(bounds: PureRect) {
    boundsFlow.emit(bounds)
  }

  private var boundsConstraints = emptyList<NSLayoutConstraint>()
  private fun getBoundsValueByIndex(index: Int) =
    boundsConstraints.getOrNull(index)?.constant?.toFloat() ?: 0f

  fun getBounds() = PureRect(
    width = getBoundsValueByIndex(0),
    height = getBoundsValueByIndex(1),
    x = getBoundsValueByIndex(2),
    y = getBoundsValueByIndex(3),
  )

  fun getPosition() = PurePoint(
    x = getBoundsValueByIndex(2),
    y = getBoundsValueByIndex(3),
  )

  fun setBoundsInMain(
    bounds: PureRect,
    rootView: UIView = uiViewControllerInMain.view,
    parentView: UIView? = rootView.superview,
  ) {
    rootView.translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.deactivateConstraints(boundsConstraints)
    NSLayoutConstraint.activateConstraints(constraints = mutableListOf(
      rootView.widthAnchor.constraintEqualToConstant(bounds.width.toDouble()),
      rootView.heightAnchor.constraintEqualToConstant(bounds.height.toDouble()),
    ).also { constraints ->
      parentView?.also {
        constraints.add(
          rootView.leftAnchor.constraintEqualToAnchor(
            anchor = parentView.leftAnchor, bounds.x.toDouble()
          )
        )
        constraints.add(
          rootView.topAnchor.constraintEqualToAnchor(
            anchor = parentView.topAnchor, bounds.y.toDouble()
          ),
        )
      }
      constraints.forEach { it.active = true }
      boundsConstraints = constraints
    })
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


  init {
    globalMainScope.launch {
      waitInit()
      val rootView = uiViewControllerInMain.view
      boundsFlow.combine(viewAppearFlow) { bounds, viewAppear ->
        bounds to rootView.superview
      }.collect { (bounds, parentView) ->
        withMainContext {
          setBoundsInMain(bounds, rootView, parentView)
        }
      }
    }
  }

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

  private val uiViewControllerDelegate = object : ComposeUIViewControllerDelegate {
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
      viewAppearFlow.value = true
    }

    // 在视图即将从视图层次结构中移除时调用
    override fun viewWillDisappear(animated: Boolean) {
      println("QWQ viewWillDisappear animated=$animated")
    }

    // 视图已经从视图层次结构中移除后会调用此函数
    override fun viewDidDisappear(animated: Boolean) {
      viewAppearFlow.value = false
      println("QWQ viewDidDisappear animated=$animated")
      scope.launch { stopSignal.emit() }
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  private val bgPlaceholderView = BgPlaceholderView()

  @OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
  @Composable
  private fun TransparentBg() {
    UIKitView(
      factory = {
        bgPlaceholderView.apply {
          setCallback { bgView ->
            bgView?.setHidden(true)
          }
        }
      },
      modifier = Modifier.fillMaxSize().zIndex(-1f),
      properties = UIKitInteropProperties(
        interactionMode = UIKitInteropInteractionMode.NonCooperative,
        isNativeAccessibilityEnabled = false
      )
    )
  }

  private var bgViewHidden by mutableStateOf(false)

  /**
   * TODO 移除 bgPlaceholderView
   */
  @OptIn(ExperimentalForeignApi::class)
  @Composable
  private fun TransparentBgV2() {
    if (bgViewHidden) {
      Box(Modifier.fillMaxSize().zIndex(-1f).drawBehind {
        drawRect(
          color = Color.Transparent,
          blendMode = BlendMode.Clear
        )
      })
    } else {
      UIKitView(
        factory = {
          bgPlaceholderView.apply {
            setCallback { bgView ->
              bgViewHidden = true
              bgView?.setHidden(true)
            }
          }
        },
      )
    }
  }

  @OptIn(
    ExperimentalForeignApi::class, ExperimentalComposeUiApi::class,
    ExperimentalComposeApi::class
  )
  val uiViewControllerInMain by lazy {
    ComposeUIViewController({
      accessibilitySyncOptions =
        when (NSProcessInfo.processInfo.arguments.contains("enabledAccessibilitySync")) {
          true -> AccessibilitySyncOptions.Always(debugLogger = null)
          else -> AccessibilitySyncOptions.WhenRequiredByAccessibilityServices(debugLogger = null)
        }
      delegate = uiViewControllerDelegate
    }) {
      /// 因为ComposeUIViewController默认是不透明的，这里放一个全局的UIView，但是它是不可见的，目的只是占位，
      /// 同时利用生命周期，获取到根部的图层，将它隐藏，从而让Pvc透明
      TransparentBg()

      LocalCompositionChain.current.Provider(
        LocalPureViewController provides this@PureViewController,
        LocalPureViewBox provides PureViewBox(LocalUIViewController.current),
      ) {
        for (content in contents) {
          content()
        }
      }
    }
  }
  val getUiViewController = SuspendOnce { withMainContext { uiViewControllerInMain } }

  private val contents = mutableStateListOf<@Composable () -> Unit>();
  override fun getContents(): MutableList<@Composable () -> Unit> {
    return contents
  }
}

fun IPureViewController.asIosPureViewController(): PureViewController {
  require(this is PureViewController)
  return this
}

class PureViewCreateParams(private val params: Map<String, Any?>) : Map<String, Any?> by params,
  IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
  override fun getBoolean(key: String): Boolean? = get(key).let { require(it is Boolean?);it }
};

package org.dweb_browser.helper.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.zIndex
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import platform.UIKit.UIView

private var vcIdAcc by atomic(0);

class PureViewController(
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
) : IPureViewController {
  constructor(params: Map<String, Any?>) : this(PureViewCreateParams(params))

  var prop = DwebUIViewControllerProperty(vcIdAcc++, -1, false)
  val vcId get() = prop.vcId

  init {
    println("QAQ init PureViewController: vcId=$vcId")
  }

  private var _isInit = false
  var isInit
    get() = _isInit
    private set(value) {
      _isInit = value
    }

  init {
    nativeRootUIViewController_onInitSignal.listen {
      if (it == vcId) {
        offListener()
        isInit = true
        initDeferred.complete(Unit)
      }
    }
    nativeRootUIViewController_onDestroySignal.listen {
      if (it == vcId) {
        destroySignal.emit()
      }
    }
  }

  private val initDeferred = CompletableDeferred<Unit>()
  suspend fun waitInit() = initDeferred.await()

  private val createSignal = Signal<IPureViewCreateParams>()

  override val onCreate = createSignal.toListener()

  private val stopSignal = SimpleSignal()

  override val onStop = stopSignal.toListener()

  private val resumeSignal = SimpleSignal()

  override val onResume = resumeSignal.toListener()

  private val destroySignal = SimpleSignal()

  override val onDestroy = destroySignal.toListener()

  private val touchSignal = Signal<TouchEvent>()
  suspend fun emitTouchSignal(ev: TouchEvent) {
    touchSignal.emit(ev)
  }

  override val onTouch = touchSignal.toListener()
  override suspend fun requestPermission(permission: String): Boolean {
    TODO("Not yet implemented requestPermission")
  }

  private val scope = nativeRootUIViewController_scope

  @OptIn(ExperimentalForeignApi::class)
  val uiViewController by lazy {
    ComposeUIViewController({
      delegate = object : ComposeUIViewControllerDelegate {
        override fun viewDidLoad() {
          scope.launch { createSignal.emit(createParams) }
        }

        override fun viewWillAppear(animated: Boolean) {
          scope.launch { resumeSignal.emit() }
        }

        override fun viewDidAppear(animated: Boolean) {
          scope.launch { stopSignal.emit() }
        }
      }
    }) {
      UIKitView(
        factory = {
          UIView().also {
            it.userInteractionEnabled = false
          }
        },
        Modifier.fillMaxSize().zIndex(0f),
        interactive = true
      )
      CompositionLocalProvider(LocalPureViewBox provides PureViewBox(LocalUIViewController.current)) {
//      DwebBrowserAppTheme {
        for (content in contents) {
          content()
        }
//      }
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
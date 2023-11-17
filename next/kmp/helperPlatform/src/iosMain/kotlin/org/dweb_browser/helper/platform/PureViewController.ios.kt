package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import platform.UIKit.UIViewController

class PureViewController(
  vc: UIViewController? = null,
  id: Int = -1,
  val createParams: PureViewCreateParams = PureViewCreateParams(mapOf())
) : IPureViewController {
  private val uiViewControllerDeferred = CompletableDeferred<UIViewController>()
  private var vcId = -1
  internal fun setVcId(id: Int) {
    vcId = id
  }

  internal fun setUIViewController(vc: UIViewController) {
    uiViewControllerDeferred.complete(vc)
  }

  init {
    if (vc != null) {
      setUIViewController(vc)
    }
    vcId = id;

    nativeRootUIViewController_onCreateSignal.listen {
      if (it == vcId) {
        createSignal.emit(createParams)
      }
    }
    nativeRootUIViewController_onPauseSignal.listen {
      if (it == vcId) {
        stopSignal.emit()
      }
    }
    nativeRootUIViewController_onResumeSignal.listen {
      if (it == vcId) {
        resumeSignal.emit()
      }
    }
    nativeRootUIViewController_onDestroySignal.listen {
      if (it == vcId) {
        destroySignal.emit()
      }
    }
  }

  suspend fun getUIViewController() = uiViewControllerDeferred.await()

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getUIViewControllerSync() =
    if (uiViewControllerDeferred.isCompleted) uiViewControllerDeferred.getCompleted() else null

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

  fun getContent() = ComposeUIViewController {
    CompositionLocalProvider(LocalPureViewBox provides PureViewBox(LocalUIViewController.current)) {
//      DwebBrowserAppTheme {
      for (content in contents) {
        content()
      }
//      }
    }
  }

  private val contents = mutableStateListOf<@Composable () -> Unit>();
  override fun getContents(): MutableList<() -> Unit> {
    return contents
  }
}

class PureViewCreateParams(private val params: Map<String, Any?>) :
  Map<String, Any?> by params, IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
  override fun getBoolean(key: String): Boolean? = get(key).let { require(it is Boolean?);it }
};
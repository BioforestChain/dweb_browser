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

class PureViewController(vc: UIViewController? = null) : IPureViewController {
  private val uiViewControllerDeferred = CompletableDeferred<UIViewController>()
  fun setUIViewController(vc: UIViewController) {
    uiViewControllerDeferred.complete(vc)
  }

  init {
    if (vc != null) {
      setUIViewController(vc)
    }
  }

  suspend fun getUIViewController() = uiViewControllerDeferred.await()

  @OptIn(ExperimentalCoroutinesApi::class)
  fun getUIViewControllerSync() =
    if (uiViewControllerDeferred.isCompleted) uiViewControllerDeferred.getCompleted() else null


  private val createSignal = Signal<IPureViewCreateParams>()

  suspend fun emitCreateSignal(/*params: PureViewCreateParams*/) {
//    createSignal.emit(params)
  }

  override val onCreate = createSignal.toListener()


  private val stopSignal = SimpleSignal()

  ////  @ObjCAction
  suspend fun emitStopSignal() {
    stopSignal.emit()
  }

  override val onStop = stopSignal.toListener()


  private val resumeSignal = SimpleSignal()

  suspend fun emitResumeSignal() {
    resumeSignal.emit()
  }

  override val onResume = resumeSignal.toListener()


  private val destroySignal = SimpleSignal()

  suspend fun emitDestroySignal() {
    destroySignal.emit()
  }

  override val onDestroy = destroySignal.toListener()


  private val touchSignal = Signal<TouchEvent>()

  suspend fun emitTouchSignal(/*ev: TouchEvent*/) {
//    touchSignal.emit(ev)
  }

  override val onTouch = touchSignal.toListener()
  override suspend fun requestPermission(permission: String): Boolean {
    TODO("Not yet implemented")
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
  override fun addContent(content: @Composable () -> Unit): () -> Boolean {
    contents.add(content)
    return {
      contents.remove(content)
    }
  }
}


class PureViewCreateParams(private val params: Map<String, Any?>) :
  Map<String, Any?> by params, IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }

  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }

  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }

  override fun getBoolean(key: String): Boolean? = get(key).let { require(it is Boolean?);it }
};
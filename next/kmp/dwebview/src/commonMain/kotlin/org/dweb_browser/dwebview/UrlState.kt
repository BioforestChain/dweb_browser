package org.dweb_browser.dwebview

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
internal class UrlState(
  val dwebView: IDWebView,
  private var startUrl: String,
  ended: Boolean = false,
) {
  private val urlLock = SynchronizedObject()
  private var endLoadUrl = CompletableDeferred<String>().also {
    if (ended) {
      it.complete(startUrl)
    }
  }
  val currentUrl get() = endLoadUrl.let { if (it.isCompleted) it.getCompleted() else startUrl }

  suspend fun awaitUrl() = endLoadUrl.await()

  fun isUrlEqual(newUrl: String): Boolean {
    if (newUrl == startUrl) {
      return true
    }
    if (endLoadUrl.isCompleted && endLoadUrl.getCompleted() == newUrl) {
      return true
    }
    return false
  }

  private fun effectWebLoadErrorState(state: WebLoadErrorState) = synchronized(urlLock) {
    debugDWebView("onLoadStateChange") {
      "WebLoadErrorState url=${state.url} error=${state.errorMessage}"
    }
    if (endLoadUrl.isActive) {
      endLoadUrl.completeExceptionally(Exception(state.errorMessage))
    }
  }

  private fun effectWebLoadStartState(state: WebLoadStartState) = synchronized(urlLock) {
    debugDWebView("WebLoadStartState") { "WebLoadStartState url=${state.url}" }
    if (state.url != startUrl || endLoadUrl.isCompleted) {
      if (endLoadUrl.isActive) {
        endLoadUrl.cancel(CancellationException("start load url: ${state.url}"))
      }
      startUrl = state.url
      endLoadUrl = CompletableDeferred()

      stateFlow.tryEmit(state.url)
    }
  }

  fun effectWebLoadSuccessState(state: WebLoadSuccessState) = synchronized(urlLock) {
    debugDWebView("WebLoadStartState") { "WebLoadSuccessState url=${state.url}" }
    endLoadUrl.complete(state.url)
    if (endLoadUrl.getCompleted() != state.url) {
      endLoadUrl = CompletableDeferred(state.url)
    }
    stateFlow.tryEmit(state.url)
  }

  init {
    dwebView.onLoadStateChange {
      when (it) {
        is WebLoadErrorState -> effectWebLoadErrorState(it)
        is WebLoadStartState -> effectWebLoadStartState(it)
        is WebLoadSuccessState -> effectWebLoadSuccessState(it)
      }
    }
  }

  suspend fun forceLoadUrl(url: String) {
    effectWebLoadStartState(WebLoadStartState(dwebView.startLoadUrl(url)))
  }

  val stateFlow = MutableStateFlow(startUrl)
}
package org.dweb_browser.dwebview

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.collectIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class UrlState(
  val dwebView: IDWebView,
) {
  private var startUrl = dwebView.loadStateFlow.value.url
  private var endLoadUrl = CompletableDeferred<String>()

  init {
    val ended = when (val state = dwebView.loadStateFlow.value) {
      is WebLoadSuccessState, is WebLoadErrorState -> true
      is WebLoadStartState -> false
    }
    if (ended) {
      endLoadUrl.complete(startUrl)
    }
  }


  private val urlLock = Mutex()
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

  private suspend fun effectWebLoadErrorState(state: WebLoadErrorState) = urlLock.withLock {
    debugDWebView("onLoadStateChange") {
      "WebLoadErrorState url=${state.url} error=${state.errorMessage}"
    }
    if (endLoadUrl.isActive) {
      endLoadUrl.completeExceptionally(Exception(state.errorMessage))
    }
  }

  private suspend fun effectWebLoadStartState(state: WebLoadStartState) = urlLock.withLock {
    debugDWebView("WebLoadStartState") { "WebLoadStartState url=${state.url}" }
    if (state.url != startUrl || endLoadUrl.isCompleted) {
      if (endLoadUrl.isActive) {
        endLoadUrl.cancel(CancellationException("start load url: ${state.url}"))
      }
      startUrl = state.url
      endLoadUrl = CompletableDeferred()

      stateFlow.value = state.url
    }
  }

  private suspend fun effectWebLoadSuccessState(state: WebLoadSuccessState) = urlLock.withLock {
    debugDWebView("WebLoadStartState") { "WebLoadSuccessState url=${state.url}" }
    endLoadUrl.complete(state.url)
    if (endLoadUrl.getCompleted() != state.url) {
      endLoadUrl = CompletableDeferred(state.url)
    }
    stateFlow.value = state.url
  }

  suspend fun forceLoadUrl(url: String) {
    effectWebLoadStartState(WebLoadStartState(dwebView.startLoadUrl(url)))
  }

  val stateFlow = MutableStateFlow(startUrl)

  init {
    dwebView.loadStateFlow.collectIn(dwebView.lifecycleScope) {
      when (it) {
        is WebLoadErrorState -> effectWebLoadErrorState(it)
        is WebLoadStartState -> effectWebLoadStartState(it)
        is WebLoadSuccessState -> effectWebLoadSuccessState(it)
      }
    }
  }
}
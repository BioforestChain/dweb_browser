package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Composable
fun IDWebView.rememberCanGoBack(): Boolean {
  val canClose = rememberCloseWatcherCanClose()
  val canBack = rememberHistoryCanGoBack()
  return canClose || canBack
}

@Composable
fun IDWebView.rememberCloseWatcherCanClose(): Boolean {
  return closeWatcher.canCloseFlow.collectAsState().value
}

@Composable
fun IDWebView.rememberHistoryCanGoBack(): Boolean {
  return produceState(false) {
    launch {
      value = historyCanGoBack()
    }
    val off = onLoadStateChange {
      value = historyCanGoBack()
    }
    awaitDispose { off() }
  }.value
}

@Composable
fun IDWebView.rememberHistoryCanGoForward(): Boolean {
  return produceState(false) {
    launch {
      value = historyCanGoForward()
    }
    val off = onLoadStateChange {
      value = historyCanGoForward()
    }
    awaitDispose { off() }
  }.value
}

@Composable
fun IDWebView.rememberLoadingProgress(): Float {
  var loadingProgress by remember {
    mutableStateOf(
      loadingProgressFlow.replayCache.lastOrNull() ?: 0f
    )
  }
  val scope = rememberCoroutineScope()
  DisposableEffect(this) {
    val job = scope.launch {
      loadingProgressFlow.collect {
        loadingProgress = it
      }
    }
    onDispose {
      job.cancel()
    }
  }
  return loadingProgress
}
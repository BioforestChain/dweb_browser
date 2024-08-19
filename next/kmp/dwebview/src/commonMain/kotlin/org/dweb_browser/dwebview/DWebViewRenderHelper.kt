package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
  LaunchedEffect(canClose, canBack) {
    debugDWebView("rememberCanGoBack") { "canClose=$canClose; canBack=$canBack" }
  }
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
    val job = launch {
      loadStateFlow.collect {
        value = historyCanGoBack()
      }
    }
    awaitDispose { job.cancel() }
  }.value
}

@Composable
fun IDWebView.rememberHistoryCanGoForward(): Boolean {
  return produceState(false) {
    launch {
      value = historyCanGoForward()
    }
    val job = launch {
      loadStateFlow.collect {
        value = historyCanGoForward()
      }
    }
    awaitDispose { job.cancel() }
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
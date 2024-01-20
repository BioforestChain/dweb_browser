package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Composable
fun IDWebView.rememberCanGoBack(): Boolean {
  var canGoBack by remember { mutableStateOf(false) }
  LaunchedEffect(this) {
    canGoBack = historyCanGoBack()
  }
  DisposableEffect(this) {
    val off = onLoadStateChange {
      canGoBack = historyCanGoBack()
    }
    onDispose { off() }
  }
  return canGoBack
}

@Composable
fun IDWebView.rememberCanGoForward(): Boolean {
  var canGoForward by remember { mutableStateOf(false) }
  LaunchedEffect(this) {
    canGoForward = historyCanGoForward()
  }
  DisposableEffect(this) {
    val off = onLoadStateChange {
      canGoForward = historyCanGoForward()
    }
    onDispose { off() }
  }
  return canGoForward
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
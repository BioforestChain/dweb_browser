package org.dweb_browser.core.ipc.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectIn(scope: CoroutineScope, collector: FlowCollector<T>) =
  scope.launch { collect(collector) }

suspend inline fun <T> Flow<T>.collectIn(collector: FlowCollector<T>) =
  coroutineScope { collectIn(this, collector) }
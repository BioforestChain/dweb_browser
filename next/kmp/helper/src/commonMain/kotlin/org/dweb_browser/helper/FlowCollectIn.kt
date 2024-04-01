package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectIn(scope: CoroutineScope, collector: FlowCollector<T>) =
  scope.launch { collect(collector) }

suspend inline fun <T> Flow<T>.collectIn(collector: FlowCollector<T>) =
  coroutineScope { collectIn(this, collector) }


/**
 * 继承 emit 所在作用域，执行 FlowCollector
 * 与常见的 Event.listen 这种模式类似
 */
fun <T> Flow<T>.listen(collector: FlowCollector<T>) = collectIn(emptyScope, collector)


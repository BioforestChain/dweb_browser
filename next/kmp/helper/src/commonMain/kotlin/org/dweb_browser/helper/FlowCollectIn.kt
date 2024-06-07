package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

inline fun <T> Flow<T>.collectIn(
  scope: CoroutineScope = globalDefaultScope,
  collector: FlowCollector<T>,
) = scope.launch(start = CoroutineStart.UNDISPATCHED) {
  collect(collector)
}

inline fun <T> Flow<T>.collectInContext(
  scope: CoroutineContext = EmptyCoroutineContext,
  collector: FlowCollector<T>,
) = collectIn(CoroutineScope(scope), collector)


enum class ScopeType {
  Main,
  IO,
  Default,
  Empty,
  ;
}

inline fun <T> Flow<T>.collectIn2(
  scopeType: ScopeType,
  collector: FlowCollector<T>,
) = collectIn(
  when (scopeType) {
    ScopeType.Main -> globalMainScope
    ScopeType.IO -> globalIoScope
    ScopeType.Default -> globalDefaultScope
    ScopeType.Empty -> globalEmptyScope
  }, collector
)

//suspend inline fun <T> Flow<T>.collectIn(collector: FlowCollector<T>) =
//  coroutineScope { this@collectIn.collectIn(this, collector) }


/**
 * 继承 emit 所在作用域，执行 FlowCollector
 * 与常见的 Event.listen 这种模式类似
 */
inline fun <T> Flow<T>.listen(
  scope: CoroutineScope = globalEmptyScope,
  collector: FlowCollector<T>,
) =
  collectIn(scope, collector)

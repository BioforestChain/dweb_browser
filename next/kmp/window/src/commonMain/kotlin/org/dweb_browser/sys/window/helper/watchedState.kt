package org.dweb_browser.sys.window.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys

/**
 * 提供一个计算函数，来获得一个在Compose中使用的 state
 */
@Composable
fun <T> WindowController.watchedState(
  key: Any? = null,
  policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
  filter: ((change: Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
  watchKey: WindowPropertyKeys? = null,
  watchKeys: Set<WindowPropertyKeys>? = null,
  getter: WindowState .() -> T,
): State<T> = remember(key) {
  val rememberState = mutableStateOf(getter.invoke(state), policy)
  val off = state.observable.onChange {
    if ((if (watchKey != null) watchKey == it.key else true) && (watchKeys?.contains(it.key) != false) && filter?.invoke(
        it
      ) != false
    ) {
      runCatching {
        rememberState.value = getter.invoke(state)
      }.onFailure {
        WARNING("watchKey=>$watchKey new:${getter.invoke(state)}")
      }
    }
  }
  Pair(rememberState, off)
}.let { (rememberState, off) ->
  DisposableEffect(off) {
    onDispose {
      off()
    }
  }
  rememberState
}
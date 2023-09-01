package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy


///**
// * 提供一个计算函数，来获得一个在Compose中使用的 state
// */
//@Composable
//fun <K:Any,T> Observable<K>.watchedState(
//  key: Any? = null,
//  policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
//  filter: ((change: Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
//  watchKey: WindowPropertyKeys? = null,
//  watchKeys: Set<WindowPropertyKeys>? = null,
//  getter: WindowState .() -> T,
//) = remember(key) {
//  mutableStateOf(getter.invoke(state), policy)
//}.also { rememberState ->
//  DisposableEffect(state) {
//    val off = state.observable.onChange {
//      if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
//          it.key
//        ) else true) && filter?.invoke(it) != false
//      ) {
//        rememberState.value = getter.invoke(state)
//      }
//    }
//    onDispose {
//      off()
//    }
//  }
//} as State<T>
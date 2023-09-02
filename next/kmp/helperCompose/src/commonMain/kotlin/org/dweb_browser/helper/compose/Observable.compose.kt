package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import org.dweb_browser.helper.Observable
import kotlin.reflect.KProperty


fun <K : Any, S : Any> Observable<K>.toComposableHelper(context: S) =
  ObservableStateComposable(context, this)

/**
 * 提供一个计算函数，来获得一个在Compose中使用的 state
 */

open class ObservableStateComposable<K : Any, S : Any>(
  val context: S,
  val observable: Observable<K>
) {
//  class ObservableComposableReadonlyContext<K : Any, S : Any, T>(
//    val key: Any? = null,
//    val policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
//    val filter: ((change: Observable.Change<K, *>) -> Boolean)? = null,
//    val watchKey: K? = null,
//    val watchKeys: Set<K>? = null,
//    val getter: S.() -> T,
//  ) {}
//
//  fun <T> readStateFactory(): @Composable (key: Any?, policy: SnapshotMutationPolicy<T>, filter: ((change: Observable.Change<K, *>) -> Boolean)?, watchKey: K?, watchKeys: Set<K>?, getter: S.() -> T) -> State<T> =
//    @Composable { key, policy, filter, watchKey, watchKeys, getter ->
//      remember(key) {
//        mutableStateOf(getter.invoke(context), policy)
//      }.also { state ->
//        DisposableEffect(state) {
//          val off = observable.onChange {
//            if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
//                it.key
//              ) else true) && filter?.invoke(it) != false
//            ) {
//              state.value = getter.invoke(context)
//            }
//          }
//          onDispose {
//            off()
//          }
//        }
//      }
//    }
//
//  val readState = readStateFactory()
  /**
   * create read only watcher
   */
  @Composable
  fun <T> stateOf(
    key: Any? = null,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    filter: ((change: Observable.Change<K, *>) -> Boolean)? = null,
    watchKey: K? = null,
    watchKeys: Set<K>? = null,
    getter: S.() -> T,
  ) = remember(key) {
    mutableStateOf(getter.invoke(context), policy)
  }.also { state ->
    DisposableEffect(state) {
      val off = observable.onChange {
        if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
            it.key
          ) else true) && filter?.invoke(it) != false
        ) {
          state.value = getter.invoke(context)
        }
      }
      onDispose {
        off()
      }
    }
  } as State<T>

  @Composable
  fun <T> mutableStateOf(
    key: Any? = null,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    filter: ((change: Observable.Change<K, *>) -> Boolean)? = null,
    watchKey: K? = null,
    watchKeys: Set<K>? = null,
    getter: S.() -> T,
    setter: S.(T) -> Unit,
  ) = remember(key) {
    val mState = mutableStateOf(getter.invoke(context), policy)
    object : MutableState<T> {
      override var value: T
        get() = mState.value
        set(value) {
          setter(context, value)
          mState.value = value
        }

      override fun component1() = mState.component1()
      override fun component2() = mState.component2()
      operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value
      operator fun setValue(
        thisObj: Any?,
        property: KProperty<*>,
        value: T
      ) {
        this.value = value
      }
    }
  }.also { state ->
    DisposableEffect(state) {
      val off = observable.onChange {
        if ((if (watchKey != null) watchKey == it.key else true) && (if (watchKeys != null) watchKeys.contains(
            it.key
          ) else true) && filter?.invoke(it) != false
        ) {
          state.value = getter.invoke(context)
        }
      }
      onDispose {
        off()
      }
    }
  }
}
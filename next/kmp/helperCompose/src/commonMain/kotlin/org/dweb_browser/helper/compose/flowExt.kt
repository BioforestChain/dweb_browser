package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun <T> MutableStateFlow<T>.collectAsMutableState(): MutableState<T> {
  val flow = this
  val state = flow.collectAsState()
  return remember(this, state) {
    object : MutableState<T> {
      override var value: T
        get() = state.value
        set(value) {
          flow.value = value
        }

      override fun component1(): T {
        return state.value
      }

      override fun component2(): (T) -> Unit {
        return { flow.value = it }
      }
    }
  }
}
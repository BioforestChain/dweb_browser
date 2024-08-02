package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun <T> MutableStateFlow<T>.collectAsMutableState(): MutableState<T> {
  val flow = this
  val innerState = remember(flow) { mutableStateOf(flow.value) }
  val outerState = remember(innerState) {
    object : MutableState<T> {
      override var value: T
        get() = innerState.value
        set(value) {
          innerState.value = value
          flow.value = value
        }

      override fun component1(): T {
        return value
      }

      override fun component2(): (T) -> Unit {
        return { value = it }
      }
    }
  }
  LaunchedEffect(flow, innerState) {
    flow.collect {
      innerState.value = it
    }
  }
  return outerState
}
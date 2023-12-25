package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow

@Stable
class IsChange(
  val needFirstCall: Boolean,
) {
  val changes: MutableState<Int> = mutableStateOf(0)

  @Composable
  fun HandleChange(onChange: @Composable () -> Unit) {
    if (changes.value > 0) {
      changes.value = 0
      onChange()
    }
  }

  @Composable
  fun <T> watchState(state: MutableState<T>): MutableState<T> {
    LaunchedEffect(state) {
      if (needFirstCall) {
        changes.value += 1
      }
      snapshotFlow {
        state.value
      }.collect {
        changes.value += 1
      }
    }

    return state
  }
}

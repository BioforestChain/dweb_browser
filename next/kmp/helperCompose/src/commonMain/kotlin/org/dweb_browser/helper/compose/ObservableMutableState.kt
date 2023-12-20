package org.dweb_browser.helper.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class ObservableMutableState<T>(
  private val state: MutableState<T>,
  private val onChange: (new: T) -> Unit
) :
  MutableState<T> by state {
  constructor(initValue: T, onChange: (new: T) -> Unit) : this(
    mutableStateOf(initValue),
    onChange
  )

  override var value: T
    get() = state.value
    set(value) {
      if (state.value != value) {
        onChange(value)
      }
      state.value = value
    }
}
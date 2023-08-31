package org.dweb_browser.helper

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@OptIn(FlowPreview::class)
suspend fun <T> debounce(
  delayMillis: Long,
  block: suspend () -> T
): T = flow { emit(block()) }.debounce(delayMillis).first()


inline fun <T> T.with(block: T.() -> Unit): T {
  with(this) {
    this.block()
  }
  return this
}
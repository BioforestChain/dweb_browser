package org.dweb_browser.helper

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

suspend fun <T> debounce(
  delayMillis: Long,
  block: suspend () -> T
): T = flow { emit(block()) }.debounce(delayMillis).first()
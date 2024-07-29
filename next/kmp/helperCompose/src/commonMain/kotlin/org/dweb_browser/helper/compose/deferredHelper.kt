package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Deferred

@Composable
fun <T> Deferred<T>.asState() = produceState<T?>(null) {
  value = await()
}
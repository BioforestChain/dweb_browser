package org.dweb_browser.helper

import kotlinx.coroutines.Deferred

/**
 * for alternative try-finally
 */
suspend fun <T> Deferred<T>.awaitResult() = runCatching { await() }
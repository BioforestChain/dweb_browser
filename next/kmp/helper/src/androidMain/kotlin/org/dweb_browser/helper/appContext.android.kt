package org.dweb_browser.helper

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

val androidAppContextDeferred = CompletableDeferred<Context>()
@OptIn(ExperimentalCoroutinesApi::class)
fun getAppContextUnsafe() = androidAppContextDeferred.getCompleted()
suspend fun getAppContext() = androidAppContextDeferred.await()

package org.dweb_browser.helper

import android.app.Activity
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

val androidAppContextDeferred = CompletableDeferred<Context>()

@OptIn(ExperimentalCoroutinesApi::class)
fun getAppContextUnsafe() = androidAppContextDeferred.getCompleted()
suspend fun getAppContext() = androidAppContextDeferred.await()

private val startActivityOptions = SafeLinkList<Pair<Activity, (() -> Bundle?)?>>()
fun addStartActivityOptions(activity: Activity, optionsBuilder: (() -> Bundle?)? = null) {
  startActivityOptions.sync {
    removeIf { it.first == activity }
    add(activity to optionsBuilder)
  }
}

fun removeStartActivityOptions(activity: Activity) {
  startActivityOptions.removeIf { it.first == activity }
}

fun getStartActivityOptions() = startActivityOptions.lastOrNull()

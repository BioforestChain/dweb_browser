package org.dweb_browser.helper

import android.app.Activity
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

public val androidAppContextDeferred: CompletableDeferred<Context> = CompletableDeferred()

@OptIn(ExperimentalCoroutinesApi::class)
public fun getAppContextUnsafe(): Context = androidAppContextDeferred.getCompleted()

public suspend fun getAppContext(): Context = androidAppContextDeferred.await()

private val startActivityOptions = SafeLinkList<Pair<Activity, (() -> Bundle?)?>>()
public fun addStartActivityOptions(activity: Activity, optionsBuilder: (() -> Bundle?)? = null) {
  startActivityOptions.sync {
    removeAll { it.first == activity }
    add(activity to optionsBuilder)
  }
}

public fun removeStartActivityOptions(activity: Activity) {
  startActivityOptions.removeAll { it.first == activity }
}

public fun getStartActivityOptions(): Pair<Activity, (() -> Bundle?)?>? =
  startActivityOptions.lastOrNull()

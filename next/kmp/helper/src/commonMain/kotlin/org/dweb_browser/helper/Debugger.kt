package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 可用值：
 *
 * "TIME-DURATION"
 *
 * "fetch"
 * "stream"
 * "native-ipc"
 * "stream-ipc"
 * "jmm"
 * "boot"
 * "http"
 * "mwebview"
 * "dwebview"
 * "js-process"
 * "message-port-ipc"
 * "ipc-body"
 * "nativeui"
 * "fetch-file"
 *
 */
private data class DebugTags(
  val debugRegexes: Set<Regex>,
  val debugNames: Set<String>,
  val verboseRegexes: Set<Regex>,
  val verboseNames: Set<String>,
  val origin: Iterable<String>,
) {
  companion object {
    var singleton: DebugTags = DebugTags(setOf(), setOf(), setOf(), setOf(), listOf())
      private set

    private val sync = SynchronizedObject()
    fun from(tags: Iterable<String>) = synchronized(sync) {
      val debugRegexes = mutableSetOf<Regex>()
      val debugNames = mutableSetOf<String>()
      val verboseRegexes = mutableSetOf<Regex>()
      val verboseNames = mutableSetOf<String>()
      val anyRegList = listOf(debugRegexes, verboseRegexes)
      val anyNamList = listOf(debugNames, verboseNames)
      val debRegList = listOf(debugRegexes)
      val debNamList = listOf(debugNames)
      val verRegList = listOf(verboseRegexes)
      val verNamList = listOf(verboseNames)
      for (tag in tags.map { it.trim() }) {
        if (tag.isEmpty()) {
          continue
        }
        var regList = debRegList
        var namList = anyNamList
        var safeTag = tag
        when {
          tag.startsWith(":debug:") -> {
            safeTag = tag.slice(":debug:".length..<tag.length)
            regList = debRegList
            namList = debNamList
          }

          tag.startsWith(":verbose:") -> {
            safeTag = tag.slice(":verbose:".length..<tag.length)
            regList = verRegList
            namList = verNamList
          }

          tag.startsWith(":all:") -> {
            safeTag = tag.slice(":all:".length..<tag.length)
            regList = anyRegList
            namList = anyNamList
          }
        }
        if (safeTag.startsWith('/') && safeTag.endsWith('/')) {
          val reg = Regex(safeTag.slice(1..<safeTag.length - 1))
          for (regexes in regList) {
            regexes.add(reg)
          }
        } else {
          for (names in namList) {
            names.add(safeTag)
          }
        }

      }
      singleton = DebugTags(debugRegexes, debugNames, verboseRegexes, verboseNames, tags)
    }

    fun add(name: String) = synchronized(sync) {
      val newNames = singleton.debugNames + name
      if (newNames != singleton.debugNames) {
        singleton = singleton.copy(debugNames = newNames)
      }
    }

    fun remove(name: String) = synchronized(sync) {
      val newNames = singleton.debugNames - name
      if (newNames != singleton.debugNames) {
        singleton = singleton.copy(debugNames = newNames)
      }
    }
  }

  private val debugResult = SafeHashMap<String, Boolean>()
  fun canDebug(scope: String) = debugResult.getOrPut(scope) {
    debugNames.contains(scope) || debugRegexes.firstOrNull { regex -> regex.matches(scope) } != null
  }

  private val verboseResult = SafeHashMap<String, Boolean>()
  fun canVerbose(scope: String) = verboseResult.getOrPut(scope) {
    verboseNames.contains(scope) || verboseRegexes.firstOrNull { regex -> regex.matches(scope) } != null
  }

  fun canTimeout(scope: String) = canDebug(scope)
}

public fun addDebugTags(tags: Iterable<String>) {
  DebugTags.from(tags)
}

public fun getDebugTags(): List<String> = DebugTags.singleton.origin.toList()

public class Debugger(public val scope: String) {
  init {
    println("Debugger scope: $scope")
  }

  public fun print(tag: String, msg: Any? = "", err: Any?, symbol: String) {
    printDebug(scope, tag, msg, err, symbol)
  }

  public inline fun print2(
    tag: String,
    err: Throwable,
    msgGetter: () -> Any?,
    symbol: String,
  ) {
    val msg = try {
      msgGetter()
    } catch (e: Throwable) {
      e
    }
    print(tag, msg, err, symbol)
  }

  public inline fun print3(tag: String, msgGetter: () -> Any?, symbol: String) {
    var err: Throwable? = null
    val msg = try {
      msgGetter()
    } catch (e: Throwable) {
      err = e
      null
    }
    print(tag, msg, err, symbol)
  }

  public operator fun invoke(tag: String, msg: Any? = "", err: Any? = null) {
    if (err != null || isEnable) {
      print(tag, msg, err, "│")
    }
  }

  public inline operator fun invoke(tag: String, err: Throwable, msgGetter: () -> Any?) {
    print2(tag, err, msgGetter, "│")
  }

  public inline operator fun invoke(tag: String, msgGetter: () -> Any?) {
    if (isEnable) {
      print3(tag, msgGetter, "│")
    }
  }

//  operator suspend fun invoke(tag: String, msgGetter: () -> Any?) {
//    if (isEnable) {
//      print3(tag, msgGetter, "│")
//    }
//  }

  public fun forceEnable(enable: Boolean = true) {
    if (enable) {
      DebugTags.add(scope)
    } else {
      DebugTags.remove(scope)
    }
  }


  public inline fun verbose(tag: String, msgGetter: () -> Any?) {
    if (isEnableVerbose) {
      print3(tag, msgGetter, "░")
    }
  }

  public fun verbose(tag: String, msg: Any? = "") {
    if (isEnableVerbose) {
      print(tag, msg, null, "░")
    }
  }

  public suspend inline fun <R> timeout(
    ms: Long,
    tag: String = "traceTimeout",
    crossinline log: () -> Any? = { "" },
    crossinline block: suspend () -> R,
  ): R = if (isEnableTimeout) {
    val timeoutJob = CoroutineScope(currentCoroutineContext()).launch {
      delay(ms);
      printDebug(scope, tag, message = lazy { log() }, error = "⏲️ TIMEOUT!!")
    }
    try {
      block()
    } finally {
      timeoutJob.cancel()
    }
  } else {
    block()
  }

  public inline fun timeout(
    scope: CoroutineScope,
    ms: Long,
    tag: String = "traceTimeout",
    crossinline log: () -> Any? = { "" },
  ): () -> Boolean = CompletableDeferred<Unit>().let { deferred ->
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      timeout(ms, tag, log) {
        deferred.await()
      }
    }

    return@let {
      deferred.complete(Unit)
    }
  }

  public var isEnableVerbose: Boolean = false
    get() {
      if (debugTags !== DebugTags.singleton) {
        debugTags = DebugTags.singleton
      }
      return field
    }
    private set
  public var isEnableTimeout: Boolean = false
    get() {
      if (debugTags !== DebugTags.singleton) {
        debugTags = DebugTags.singleton
      }
      return field
    }
    private set
  public var isEnable: Boolean = false
    get() {
      if (debugTags !== DebugTags.singleton) {
        debugTags = DebugTags.singleton
      }
      return field
    }
    private set
  private var debugTags: DebugTags? = null
    set(value) {
      if (value !== field) {
        if (value == null) {
          isEnableVerbose = false
          isEnableTimeout = false
          isEnable = false
        } else {
          isEnableVerbose = value.canVerbose(scope)
          isEnableTimeout = value.canTimeout(scope)
          isEnable = value.canDebug(scope)
        }
        field = value
      }
    }
}

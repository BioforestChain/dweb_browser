package org.dweb_browser.helper

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections


object StateShared {
  val obsStack = mutableListOf<StateBase>()
}

abstract class StateBase {

  val deps = Collections.synchronizedSet(HashSet<StateBase>());

  val refs = Collections.synchronizedSet(HashSet<StateBase>());

  fun addDep(dep: StateBase): Boolean {
    val success = deps.add(dep)
    if (success) {
      dep.addRef(this)
    }
    return success
  }

  fun addRef(ref: StateBase): Boolean {
    val success = refs.add(ref)
    if (success) {
      ref.addDep(this)
    }
    return success
  }

  fun rmDep(dep: StateBase): Boolean {
    val success = deps.remove(dep)
    if (success) {
      dep.rmRef(this)
    }
    return success
  }

  fun rmRef(ref: StateBase): Boolean {
    val success = refs.remove(ref)
    if (success) {
      ref.rmDep(this)
    }
    return success
  }

  fun clearDeps() {
    deps.toTypedArray().forEach { dep ->
      rmDep(dep)
    }
  }

  abstract suspend fun beGet(force: Boolean? = null)
}

@Suppress("UNCHECKED_CAST")
class State<T>(private var defaultValue: T) : StateBase() {

  fun setter(newValue: T): Boolean {
    cache = defaultValue;
    hasCache = true;

    return if (this.defaultValue == newValue) false
    else {
      defaultValue = newValue
      true
    }
  }

  fun getter(): T {
    // 如果是闭包
    if (defaultValue is Function<*>) {
      val runFun = defaultValue as () -> T
      return runFun()
    }
    return defaultValue
  }

  init {
    setter(defaultValue)
  }

  var cache: T? = null
  private var hasCache = false

  // 第一个t是新值，第二个t是旧值
  private val changeSignals = Signal<Pair<T, T?>>()

  var onChange = changeSignals.toListener()

  suspend fun getNext(): T {
    val res = PromiseOut<T>()
    onChange { newData ->
      res.resolve(newData.first)
    }
    return res.waitPromise()
  }

  suspend fun until(checker: (T) -> Boolean): T? {
    if (checker(get())) {
      return null
    }
    val locker = PromiseOut<T>()
    onChange { newData ->
      locker.resolve(newData.first)
    }
    return locker.waitPromise()
  }

  @OptIn(InternalCoroutinesApi::class)
  fun get(force: Boolean? = null): T {
    synchronized(StateShared.obsStack) {
      val caller = StateShared.obsStack.lastOrNull()
      if (caller !== null) {
        caller.addDep(this)
      }
    }
    val inForce = if (force == null) hasCache else true
    if (inForce) {
      /// 自己也将作为调用者
      synchronized(StateShared.obsStack) {
        StateShared.obsStack.add(this)
      }
      /// 调用之前，清空自己的依赖，重新收集依赖
      clearDeps()
      try {
        val oldValue = cache
        cache = getter()
        hasCache = true
        runBlocking { changeSignals.emit(Pair(cache!!, oldValue)) }
        return cache!!
      } finally {
        /// 移除自己作为调用者的身份
        synchronized(StateShared.obsStack) {
          StateShared.obsStack.remove(this)
        }
      }
    }
    return cache!!
  }

  suspend fun set(value: T, force: Boolean = false): Boolean {
    if (setter(value) || force) {
      /// 强制更新值
      get(true)
      /// 向自己的调用者发去通知
      for (ref in refs) {
        ref.beGet(true)
      }
      return true
    }
    return false
  }

  suspend fun updateT(updater: (T?) -> T, force: Boolean = true): Boolean {
    return set(updater(cache), force)
  }

  suspend fun update(updater: (T?) -> Boolean): Boolean {
    if (cache != null) {
      return set(cache!!, updater(cache))
    }
    return false
  }

  suspend fun updateUnit(updater: (T?) -> Unit, force: Boolean = true): Boolean {
    updater(cache)
    if (cache != null) {
      return set(cache!!, force)
    }
    return false
  }

  suspend fun toStream(): Sequence<T> = sequence {
    var waitter: PromiseOut<T>? = null
    val locker = Mutex()
    val cacheList = mutableListOf<T>()
    onChange { data ->
      locker.withLock {
        waitter?.let {
          it.resolve(data.first)
          waitter = null
        }
        if (waitter == null) {
          cacheList.add(data.first)
        }
      }
    }
    try {
      val tmp = runBlocking { get() }
      yield(tmp)
      while (true) {
        while (true) {
          var success: Boolean
          var item: T?
          runBlocking {
            locker.withLock {
              item = cacheList.removeFirst()
              success = (item != null)
            }
          }
          if (!success) {
            break;
          }
          return@sequence yield(item!!)
        }
        runBlocking { locker.lock() }
        val w = PromiseOut<T>()
        waitter = w
        locker.unlock()
        val res = runBlocking { w.waitPromise() }
        return@sequence yield(res)
      }
    } finally {
      locker.unlock()
    }
  }

  override suspend fun beGet(force: Boolean?) {
    get(force)
  }

}
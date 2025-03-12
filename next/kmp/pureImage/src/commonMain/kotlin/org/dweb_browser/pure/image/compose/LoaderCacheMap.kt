package org.dweb_browser.pure.image.compose

import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeLinkList
import org.dweb_browser.helper.trueAlso

data class CacheItem<T>(
  val task: LoaderTask,
  private val _result: T,//,
) {
  val key get() = task.key
  val result get() = _result.also { hot = 30f }
  internal var hot = 30f
}

class LoaderCacheMap<T : Any>(scope: CoroutineScope, var cacheSize: Int = 30) {
  private val map = SafeHashMap<String, CacheItem<T>>()
  private val lruList = SafeLinkList<CacheItem<T>>()

  fun get(task: LoaderTask): T? =
    map[task.key]?.let {
      lruList.remove(it)
      lruList.add(0, it)
      it.result
    }

  fun save(cache: CacheItem<T>) {
    map[cache.key] = cache
    lruList.add(0, cache)
    while (map.size > cacheSize) {
      val willRemove = lruList.removeLast()
      map.remove(willRemove.key)
    }
  }

  fun delete(task: LoaderTask, result: CacheItem<T>? = null) {
    if (result == null) {
      map.remove(task.key)?.also {
        lruList.remove(it)
      }
    } else {
      map.remove(task.key, result).trueAlso {
        lruList.remove(result)
      }
    }
  }
}
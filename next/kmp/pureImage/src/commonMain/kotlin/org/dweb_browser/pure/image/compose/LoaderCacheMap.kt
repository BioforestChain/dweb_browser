package org.dweb_browser.pure.image.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashMap

data class CacheItem<T>(
  val task: LoaderTask,
  private val _result: T,//,
) {
  val key get() = task.key
  val result get() = _result.also { hot = 30f }
  internal var hot = 30f
}

class LoaderCacheMap<T : Any>(scope: CoroutineScope, cacheSize: Int = 10) {
  private val map = SafeHashMap<String, CacheItem<T>>()

  init {
    scope.launch {
      while (true) {
        delay(5000)
        if (map.size <= cacheSize) {
          return@launch
        }
        val willRemoves = mutableListOf<MutableMap.MutableEntry<String, CacheItem<T>>>()
        map.sync {
          for (item in map) {
            item.value.hot -= 5f
            if (item.value.hot <= 0) {
              willRemoves.add(item)
            }
          }
          willRemoves.sortBy { it.value.hot }
          for ((key) in willRemoves) {
            this.remove(key)
            if (this.size <= cacheSize) {
              break
            }
          }
        }
      }
    }
  }

  fun get(task: LoaderTask): T? =
    map[task.key]?.result

  fun save(cache: CacheItem<T>) {
    map[cache.key] = cache
  }

  fun delete(task: LoaderTask, result: CacheItem<T>? = null) {
    if (result == null) {
      map.remove(task.key)
    } else {
      map.remove(task.key, result)
    }
  }
}
package org.dweb_browser.dwebview.engine

class Extends<T : Any> {
  class Config(val order: Int = 0)

  fun add(instance: T, config: Config = Config()): Boolean {
    if (instanceMap[instance] == config) {
      return false
    }
    instanceMap[instance] = config
    resetCacheBy(instance)
    return true
  }

  fun remove(instance: T): Boolean {
    return instanceMap.remove(instance)?.let {
      resetCacheBy(instance)
      true
    } ?: false
  }

  private val instanceMap = mutableMapOf<T, Config>()
  private val methodCache = mutableMapOf<String, List<T>>()
  private fun resetCacheBy(instance: T) {
    for (method in instance.javaClass.declaredMethods) {
      methodCache.remove(method.name)
    }
  }

  fun hasMethod(methodName: String) = methodCache.getOrPut(methodName) {
    instanceMap.filter { isOverriding(it.key, methodName) }.toList().sortedBy { it.second.order }
      .map { it.first }
  }

  private fun isOverriding(instance: T, methodName: String): Boolean {
    return instance.javaClass.declaredMethods.any { it.name == methodName }
  }
}
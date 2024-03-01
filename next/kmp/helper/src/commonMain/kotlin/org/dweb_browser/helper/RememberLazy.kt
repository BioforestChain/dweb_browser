package org.dweb_browser.helper

class RememberLazy<T : Any?>(key: Any?, private val initializer: () -> T) : MemoryChain(key),
  Lazy<T> {
  private var _lazy = lazy(initializer)

  init {
    onChange {
      if (_lazy.isInitialized()) {
        _lazy = lazy(initializer)
      }
    }
  }

  fun <T : Any?> then(key: Any? = null, initializer: () -> T) =
    RememberLazy(key, initializer).also { it.follow(this) }


  val lazy: Lazy<T>
    get() = _lazy
  override val value: T
    get() = lazy.value

  override fun isInitialized(): Boolean = lazy.isInitialized()
}

open class MemoryChain(key: Any?) {
  private val leaders = mutableSetOf<MemoryChain>()
  private val followers = mutableSetOf<MemoryChain>()
  fun follow(leader: MemoryChain) {
    leader.followers.add(this)
    leaders.add(leader)
  }

  fun unfollow(leader: MemoryChain) {
    leader.followers.remove(this)
    leaders.remove(leader)
  }

  private var _key = key
  val key get() = _key
  suspend fun setKey(value: Any?) {
    if (value != _key) {
      _key = value
      emitChanges()
    }
  }

  /**
   * 触发变更，跟随者递归该触发行为
   */
  private suspend fun emitChanges() {
    onChangeSignal.emit()
    for (follower in followers) {
      follower.emitChanges()
    }
  }

  private val onChangeSignal = SimpleSignal()
  val onChange = onChangeSignal.toListener()
}
package org.dweb_browser.helper

public class RememberLazy<T : Any?>(key: Any?, private val initializer: () -> T) : MemoryChain(key),
  Lazy<T> {
  private var _lazy = lazy(initializer)

  init {
    onChange {
      if (_lazy.isInitialized()) {
        _lazy = lazy(initializer)
      }
    }
  }

  public fun <R : Any?> then(key: Any? = null, initializer: () -> R): RememberLazy<R> =
    RememberLazy(key, initializer).also { it.follow(this) }

  public val lazy: Lazy<T>
    get() = _lazy
  override val value: T
    get() = lazy.value

  override fun isInitialized(): Boolean = lazy.isInitialized()
}

public open class MemoryChain(key: Any?) {
  private val leaders = mutableSetOf<MemoryChain>()
  private val followers = mutableSetOf<MemoryChain>()
  public fun follow(leader: MemoryChain) {
    leader.followers.add(this)
    leaders.add(leader)
  }

  public fun unfollow(leader: MemoryChain) {
    leader.followers.remove(this)
    leaders.remove(leader)
  }

  private var _key = key
  public val key: Any? get() = _key
  public suspend fun setKey(value: Any?) {
    if (value != _key) {
      _key = value
      emitChanges()
    }
  }


  private val onChangeSignal = SimpleSignal()
  public val onChange: Signal.Listener<Unit> = onChangeSignal.toListener()

  /**
   * 触发变更，跟随者递归该触发行为
   */
  private suspend fun emitChanges() {
    onChangeSignal.emit()
    for (follower in followers) {
      follower.emitChanges()
    }
  }

}
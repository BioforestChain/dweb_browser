package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class ReentrantReadWriteLock {
  public val writeLock: Mutex = Mutex()
  private val readLock = Mutex()
  private var readers = 0
  public suspend fun readStart() {
    readLock.withLock {
      if (readers == 0) {
        writeLock.lock()  // 第一个读者锁定写锁
      }
      readers++
    }
  }

  public suspend fun readEnd() {
    readLock.withLock {
      readers--
      if (readers == 0) {
        writeLock.unlock()  // 最后一个读者释放写锁
      }
    }
  }

  public suspend inline fun <T> read(block: () -> T): T {
    readStart()

    try {
      return block()
    } finally {
      readEnd()
    }
  }

  public suspend inline fun <T> write(block: () -> T): T {
    writeLock.withLock {
      return block()
    }
  }
}
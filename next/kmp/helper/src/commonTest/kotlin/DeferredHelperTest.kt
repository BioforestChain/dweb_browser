package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


class DeferredHelperTest {
  @Test
  fun DeferredSignalTest() = runCommonTest {
    for (i in 1..100000) {
      println("test-$i")
      val deferred = CompletableDeferred<Int>()
      val signal = DeferredSignal(deferred)
      var a by atomic("")
      signal {
        a += "signal emitted"
      }
      val lock = Mutex()
      launch {
        lock.withLock {
          deferred.complete(1)
          a += " done"
        }
      }

      deferred.await()
      lock.withLock {
        assertEquals(a, "signal emitted done")
      }
    }
  }
}
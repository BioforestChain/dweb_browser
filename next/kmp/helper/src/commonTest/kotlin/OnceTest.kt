package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Once
import org.dweb_browser.helper.Once1
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnceTest {
  @Test
  fun SuspendOnceTest() = runCommonTest {
    val a = atomic(0)
    val onceFun = SuspendOnce {
      a.incrementAndGet()
    }
    coroutineScope {
      for (i in 1..10) {
        launch {
          onceFun()
        }
      }
    }
    assertEquals(a.value, 1)
  }

  @Test
  fun SuspendOnceResetTest() = runCommonTest {
    val onceFun = SuspendOnce {
      delay(100)
      123
    }
    onceFun()
    onceFun.reset()
  }

  @Test
  fun SuspendOnceReset2Test() = runCommonTest(1000) {
    val onceFun = SuspendOnce {
      delay(10)
      throw Exception("xxx")
    }
    launch { onceFun.reset() }
    assertTrue(runCatching { onceFun() }.isFailure)
  }

  @Test
  fun SuspendOnce1Test() = runCommonTest {
    val a = atomic(0)
    val onceFun = SuspendOnce1 { it: Unit ->
      a.incrementAndGet()
    }
    coroutineScope {
      for (i in 1..10) {
        launch {
          onceFun(Unit)
        }
      }
    }
    assertEquals(a.value, 1)
  }

  @Test
  fun OnceTest() = runCommonTest {
    val a = atomic(0)
    val onceFun = Once {
      a.incrementAndGet()
    }
    coroutineScope {
      for (i in 1..10) {
        launch {
          onceFun()
        }
      }
    }
    assertEquals(a.value, 1)
  }

  @Test
  fun Once1Test() = runCommonTest {
    val a = atomic(0)
    val onceFun = Once1 { it: Unit ->
      a.incrementAndGet()
    }
    coroutineScope {
      for (i in 1..10) {
        launch {
          onceFun(Unit)
        }
      }
    }
    assertEquals(a.value, 1)
  }

}
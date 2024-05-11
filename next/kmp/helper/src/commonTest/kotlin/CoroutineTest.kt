package info.bagen.dwebbrowser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.await
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoroutineTest {

  @Test
  fun jobTest() = runCommonTest {
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.cancel(CancellationException("qaq"))
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNull(err)
    }
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.completeExceptionally(Exception("qzq"))
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNotNull(err)
    }
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.cancel()
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNull(err)
    }
  }


  // @Test
  fun testThrowInLaunchWithSupervisorJob() = runCommonTest {
    val job = launch(SupervisorJob()) {
      delay(100)
      throw Exception("qaq")
      null
    }
    assertTrue(runCatching { job.join() }.isSuccess)
    assertTrue(runCatching { job.await() }.isFailure)
  }

  // @Test
  fun testThrowInLaunch() = runCommonTest {
    val job = launch {
      delay(100)
      throw Exception("qaq")
      null
    }
    assertTrue(runCatching { job.join() }.isFailure)
  }
//  // @Test
//  fun testThrowInLaunch() = runCommonTest {
//    val job = launch {//(SupervisorJob())
//      delay(100)
//      throw Exception("qaq")
//    }
//    assertTrue(runCatching { job.join() }.isFailure)
//    assertTrue(runCatching { job.await() }.isFailure)
//  }
//
//  // @Test
//  fun testCancelInLaunch() = runCommonTest {
//    val job = launch {//(SupervisorJob())
//      delay(100)
//      cancel(CancellationException("qaq"))
//    }
//    assertTrue(runCatching { job.join() }.isFailure)
//    assertTrue(runCatching { job.await() }.isFailure)
//  }

  // @Test
  fun testThrowInAsyncWithSupervisorJob() = runCommonTest {
    val deferred = async(SupervisorJob()) {
      delay(100)
      throw Exception("qaq")
      null
    }
    assertTrue(runCatching { deferred.join() }.isSuccess)
    assertTrue(runCatching { deferred.await() }.isFailure)
  }

  // @Test
  fun testThrowInAsync() = runCommonTest {
    val deferred = async {
      delay(100)
      throw Exception("qaq")
      null
    }
    assertTrue(runCatching { deferred.await() }.isFailure)
  }

  // @Test
  fun testCancelInAsync() = runCommonTest {
    val deferred = async {
      delay(100)
      cancel(CancellationException("qaq"))
    }
    assertTrue(runCatching { deferred.join() }.isFailure)
    assertTrue(runCatching { deferred.await() }.isFailure)
  }

  @Test
  fun testJobSelfCancel() = runCommonTest {
    val scope = CoroutineScope(ioAsyncExceptionHandler + SupervisorJob())
    var res = 0
    scope.launch {
      res++
      println("cancel start")
      scope.cancel()
      println("cancel end")
      res++
    }.join()
    assertEquals(res, 2)
  }


  @Test
  fun testJobSelfCancelOnce() = runCommonTest {
    val scope = CoroutineScope(ioAsyncExceptionHandler + SupervisorJob())
    var res = 0
    val cancel = SuspendOnce {
      res++
      println("cancel start")
      scope.cancel()
      println("cancel end")
      res++
    }
    launch {
      cancel()
      cancel()
    }.join()
    assertEquals(res, 2)
  }


}
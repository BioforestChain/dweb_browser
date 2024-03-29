package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.listen
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowTest {

  @Test
  fun sharedIn() = runCommonTest {
    val scope = CoroutineScope(SupervisorJob())
    run {
      val source = MutableSharedFlow<Int>(replay = 10)
      source.emit(1)
      val lazily = source.onEach { println("in $it") }.shareIn(scope, SharingStarted.Eagerly, 1)
      source.emit(2)
      lazily.collectIn(scope) {
        println("eagerly - $it")
      }
      delay(100)
      source.emit(3)
      delay(100)

    }
    run {
      val source = MutableSharedFlow<Int>(replay = 10)
      val lazily = source.shareIn(scope, SharingStarted.Lazily, 1)
      source.emit(1)
      source.emit(2)
      lazily.collectIn(scope) {
        println("lazily - $it")
      }
      delay(100)
      source.emit(3)
      delay(100)
    }

    scope.cancel()
  }

  @Test
  fun mutableSharedFlow() = runCommonTest {
    val scope = CoroutineScope(SupervisorJob())
    val messageFlow = MutableSharedFlow<Int>()
    val onMessage = messageFlow.shareIn(scope, SharingStarted.Eagerly)

    /**
     * MutableSharedFlow onBufferOverflow = BufferOverflow.SUSPEND
     * 这种模式虽然意味着 emit 会阻塞，但是必须至少要有一个 subscriber，否则 emit 就会丢失
     *
     * 这时候 messageFlow.shareIn(scope, SharingStarted.Lazily) 的创建并不会生成 subscriber
     * 这里的 SharingStarted.Lazily 意味着只有 onMessage 被 collect 了，才会开始去与源头的 messageFlow 进行 collect
     * 也就意味着 onMessage 如果没 collect ，那么 messageFlow 的 emit 就无法正确阻塞
     *
     * 如果换成 messageFlow.shareIn(scope, SharingStarted.Eagerly)
     * 那么 messageFlow 会被 onMessage 阻塞
     * 但关键在于没人阻塞 onMessage，那么自然 messageFlow 还是没有人会阻塞
     *
     *
     * 我们的最终目标是达成一个完全的流式处理，能全链路反压
     * 而反压的重点在于它要能知道其下游在阻塞它，它才能反过来阻塞上游
     * 而 sharedFlow 则是反其特性的存在，因为它本身是一个 1:M(M>=0) 的 模式
     *    如果M>0，那么任何一个订阅者都能反压它
     *    如果M=0，那么没有订阅者等于没有反压
     */

    val MAX = 5;

    val job = launch {
      println("prepare")
      onMessage.collect {
        println("collect($it)")
        delay(1000)
        if (it == MAX) {
          currentCoroutineContext().cancel()
        }
      }
    }

//    delay(100)
    for (i in 1..MAX) {
      messageFlow.emit(i)
      println("emit($i)")
    }
    job.join()
  }

  @Test
  fun eventEmitter() = runCommonTest {
    val MAX = 5;
    val emitter = MutableSharedFlow<Int>().onSubscription {
      println("onSubscription")
//    delay(100)
      for (i in 1..MAX) {
        emit(i)
        println("emit($i)")
      }
    }

    val onMessage = emitter.shareIn(this, SharingStarted.Lazily)
    val job = launch {
      println("prepare")
      delay(1000)
      onMessage.collect {
        println("collect($it)")
        delay(1000)
        if (it == MAX) {
          currentCoroutineContext().cancel()
        }
      }
    }


    job.join()
  }

  @Test
  fun channelFlow() = runCommonTest {
    val channel = Channel<Int>(capacity = Channel.UNLIMITED)
    val MAX = 5;

    for (i in 1..MAX) {
      channel.trySend(i)
      println("trySend($i)")
    }

    val flow = channel.consumeAsFlow()
    /// 这里 collect 两次会异常
    launch {
      flow.collect {
        println("collect1($it)")
      }
    }
    launch {
      flow.collect {
        println("collect2($it)")
      }
    }
  }


  @Test
  fun channelConsumeAsFlowSharedIn() = runCommonTest {
    val channel = Channel<Int>()
    val MAX = 5;

    launch {
      /// 所有的send，并不会被 collect 阻塞，consumeAsFlow 已经将它全部消费
      delay(1000)
      println("start send")
      for (i in 1..MAX) {
        channel.send(i)
        println("send($i)")
      }
    }
    val flow = channel.consumeAsFlow().shareIn(this, SharingStarted.Lazily)

    launch {
      flow.collect {
        println("collect1($it)")
        delay(1000)
      }
    }
    launch {
      delay(2000)
      flow.collect {
        println("collect2($it)")
      }
    }
  }

  @Test
  fun channelReceiveAsFlowSharedIn() = runCommonTest {
    val channel = Channel<Int>()
    val MAX = 5;

    launch {
      /// 所有的send，并不会被 collect 阻塞，consumeAsFlow 已经将它全部消费
      delay(1000)
      println("start send")
      for (i in 1..MAX) {
        channel.send(i)
        println("send($i)")
      }
    }
    val flow = channel.receiveAsFlow().shareIn(this, SharingStarted.Lazily)

    launch {
      flow.collect {
        println("collect1($it)")
        delay(1000)
      }
    }
    launch {
      delay(2000)
      flow.collect {
        println("collect2($it)")
      }
    }
  }

  @Test
  fun flowChannelSharedIn() = runCommonTest {
    val MAX = 5;
    val channel = kotlinx.coroutines.flow.channelFlow<Int> {
      println("start send")
      for (i in 1..MAX) {
        channel.send(i)
        println("send($i)")
      }
    }

    val flow = channel.shareIn(this, SharingStarted.Lazily)

    launch {
      delay(1000)
      flow.collect {
        println("collect1($it)")
        delay(1000)
      }
    }
    launch {
      delay(2000)
      flow.collect {
        println("collect2($it)")
      }
    }
  }

  @Test
  fun unlimitedChannelSharedIn() = runCommonTest {
    val channel = Channel<Int>(capacity = Channel.UNLIMITED)
    val MAX = 5;

    for (i in 1..MAX) {
      channel.trySend(i)
      println("trySend($i)")
    }
    val flow = channel.consumeAsFlow().shareIn(this, SharingStarted.Lazily)

    launch {
      flow.collect {
        println("collect1($it)")
      }
    }
    launch {
      delay(100)
      flow.collect {
        println("collect2($it)")
      }
    }
  }

  @Test
  fun flowListen() = runCommonTest(10000) { time ->
    println("job-$time start")
    val channel = Channel<Int>()
    val MAX = 5;
    val result = atomic(0)
    val flow = channel.consumeAsFlow().shareIn(this, SharingStarted.Lazily)
    val job = flow.listen {
      val res = result.addAndGet(it)
      // println("collect($it)=>$res")
      if (it == MAX) {
        currentCoroutineContext().cancel()
      }
    }

    launch {
      // println("send start")
      for (i in 1..MAX) {
        channel.send(i)
        // println("send($i)")
      }
      // println("send end")
    }

    job.invokeOnCompletion {
      println("job-$time complete")
      assertEquals(15, result.value)
      channel.close()
    }
  }
}
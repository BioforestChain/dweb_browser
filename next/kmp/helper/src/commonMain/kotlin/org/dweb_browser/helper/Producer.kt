package org.dweb_browser.helper

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 消息生产者，彻底的消费掉消息需要显示调用consume()
 */
class Producer<T>(val name: String, parentScope: CoroutineScope) {
  private val job = SupervisorJob(parentScope.coroutineContext[Job])
  val scope = parentScope + job
  val debugProducer by lazy { Debugger(this.toString()) }
  val await = job::await

  private val consumers = SafeHashSet<Consumer>()
  private val buffers = SafeLinkList<Event>()
  private val eventChannel = Channel<Event>(capacity = Channel.BUFFERED)
  private val eventLock = OrderDeferred()
  private val eventLoopJob = scope.launch {
    for (event in eventChannel) {
      launch(start = CoroutineStart.UNDISPATCHED) {
        doEmit(event)
      }
    }
  }.also {
    it.invokeOnCompletion {
      scope.launch {
        this@Producer.close()
      }
    }
  }

  override fun toString(): String {
    return "Producer<$name>"
  }

  /**生产者构造的事件*/
  inner class Event(val data: T, order: Int?, private val eventJob: CompletableJob = Job()) :
    OrderBy, Job by eventJob {
    override val order = when {
      order == null && data is OrderBy -> data.order
      else -> order
    }
    var consumed = false
      private set
    internal val emitLock = Mutex()
    fun consume(): T {
      if (!consumed) {
        consumed = true
        buffers.remove(this)
        eventJob.complete()
      }
      return data
    }

    /**
     * TODO
     */
    suspend fun next() {
    }

    /**将其消耗转换为R对象 以返回值形式继续传递*/
    inline fun <reified R : T> consumeAs(): R? {
      if (R::class.isInstance(data)) {
        return consume() as R
      }
      return null
    }

    inline fun consumeFilter(filter: (T) -> Boolean): T? {
      if (filter(data)) {
        return consume()
      }
      return null
    }

    /**将其消耗转换为R对象 以回调形式继续传递*/
    inline fun <reified R : T> consumeAs(block: (R) -> Unit) {
      if (R::class.isInstance(data)) {
        block(consume() as R)
      }
    }

    override fun toString(): String {
      return "Event($data)"
    }

    /**按顺序触发事件*/
    internal suspend inline fun orderInvoke(crossinline invoker: suspend () -> Unit) {
      orderInvoker.tryInvoke(order, key = this) {
        invoker()
      }
    }


    internal suspend fun emitBy(consumer: Consumer) {
      emitLock.withLock {
        if (consumed) {
          return@withLock
        }
        var i = 0;
        // 事件超时告警
        val job = scope.launch {
          delay(1000)
          WARNING("emitBy TIMEOUT!! step=$i consumer=$consumer data=$data")
        }
        i = 1
        emitJobsLock.lock()
        i = 2
        consumer.input.send(this)
        i = 3
        // 等待事件执行完成在往下走
        emitJobsLock.withLock {
          i = 3
          emitJobs.joinAll()
          i = 4
          emitJobs.clear()
        }
        job.cancel()
        if (consumed) {
          debugProducer("emitBy", "consumer=$consumer consumed data=$data")
        }
      }
    }

    internal val emitJobs = SafeLinkList<Job>()
    internal val emitJobsLock = Mutex()
  }

  private val orderInvoker = OrderInvoker()

  @OptIn(DelicateCoroutinesApi::class)
  private fun ensureOpen() {
    if (isClosedForSend) {
      throw Exception("$this already close for emit.")
    }
  }

  suspend fun send(value: T, order: Int? = null) = eventLock.withLock {
    ensureOpen()
    val event = Event(value, order)
    buffers.add(event)
    if (buffers.size > 10) {
      WARNING("$this buffers overflow maybe leak: $buffers")
    }
    eventChannel.send(event)
  }

  suspend fun sendBeacon(value: T, order: Int? = null) = eventLock.withLock {
    val event = Event(value, order)
    doEmit(event)
    when {
      event.consumed -> debugProducer("sendBeacon", event)
      else -> debugProducer("lostBeacon", event)
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private suspend fun doEmit(event: Event) {
    event.orderInvoke {
      withScope(scope) {
        for (consumer in consumers.toList()) {
          if (!consumer.started || consumer.startingBuffers?.contains(event) == true || consumer.input.isClosedForSend) {
            continue
          }
          event.emitBy(consumer)
          if (event.consumed) {
            break
          }
        }
      }
    }
  }

  /**事件消耗器，调用后事件将被彻底消耗，不会再进行传递*/
  fun consumer(name: String): Consumer {
    ensureOpen()
    return Consumer(name)
  }

  inner class Consumer internal constructor(
    val name: String,
//    internal val input: MutableSharedFlow<Event> = MutableSharedFlow(),
    internal val input: Channel<Event> = Channel(),
  ) : Flow<Event> by input.consumeAsFlow() {
    val debugConsumer by lazy { Debugger(this.toString()) }
    override fun toString(): String {
      return "Consumer<[$producerName]$name>"
    }

    val producerName get() = this@Producer.name

    var started = false
      private set

    var startingBuffers: List<Event>? = null

    private val start = SuspendOnce {
      withScope(scope) {
        started = true
        val starting = buffers.toList()
        startingBuffers = starting
        debugConsumer("startJob", "begin $starting")
        /// 将之前没有被消费的逐个触发，这里不用担心 buffers 被中途追加，emit会同步触发
        for (event in starting) {
          launch(start = CoroutineStart.UNDISPATCHED) {
            event.orderInvoke {
              event.emitBy(this@Consumer)
            }
          }
        }
        debugConsumer("startJob", "done")
        startingBuffers = null
      }
    }

    override suspend fun collect(collector: FlowCollector<Event>) {
      val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        for (event in input) {
          // 同一个事件的处理，不做任何阻塞，直接发出
          event.emitJobs += launch {
            println("QAQ emit ${this@Consumer}>>$collector 1")
            collector.emit(event)
            println("QAQ emit ${this@Consumer}>>$collector 2")
          }
          // 告知完成了，放行
          event.emitJobsLock.unlock()
        }
      }
      // 事件在收集了再调用开始
      start()
      job.join()
    }

    init {
      consumers.add(this)
      invokeOnClose {
        cancel()
      }
    }

    fun cancel() {
      start.reset()
      consumers.remove(this)
    }
  }

  var isClosedForSend = false
    private set

  /**关闭写，这个将会消耗完没有消费的*/
  suspend fun closeWrite(cause: Throwable? = null) {
    if (isClosedForSend) {
      return
    }
    debugProducer("closeWrite", cause)
    isClosedForSend = true
    val bufferEvents = buffers.toList()
    debugProducer("closeEvents", bufferEvents)
    for (event in bufferEvents) {
      scope.launch(start = CoroutineStart.UNDISPATCHED) {
        event.orderInvoke {
          if (!event.consumed) {
            event.consume()
            debugProducer("closeWrite", "event=$event consumed by close")
          }
        }
      }
    }
    debugProducer("closeWrite", "eventLoopJob.cancel")
    eventLock.withLock {
      eventChannel.close(cause)
    }
    debugProducer("closeWrite", "eventLoopJob.canceld")
  }

  suspend fun close(cause: Throwable? = null) {
    closeWrite(cause)
    eventLoopJob.join()

    // 等待消费者全部完成
    buffers.toList().joinAll()

    debugProducer("close", "close consumers")
    // 关闭消费者channel，表示彻底无法再发数据
    for (consumer in consumers) {
      consumer.input.close(cause)
    }
    scope.close()
    debugProducer("close", "free memory")
    consumers.clear()
    buffers.clear()
  }

  fun invokeOnClose(handler: CompletionHandler) {
    scope.coroutineContext[Job]!!.invokeOnCompletion(handler)
  }
}

/**创建一个新的消耗者继续传递*/
fun <T> Flow<T>.asProducer(
  name: String,
  scope: CoroutineScope,
  emitter: suspend Producer<T>.(T) -> Unit = { send(it) },
) = Producer<T>(name, scope).also { producer ->
  collectIn(producer.scope) {
    producer.emitter(it)
  }.invokeOnCompletion {
    producer.scope.launch {
      producer.close()
    }
  }
}

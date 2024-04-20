package org.dweb_browser.helper

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
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

  //  private val eventChannel = Channel<Event>(capacity = Channel.BUFFERED)
  private val actionQueue = OrderDeferred()
//  private val eventLoopJob = scope.launch {
//    for (event in eventChannel) {
//      launch(start = CoroutineStart.UNDISPATCHED) {
//        doEmit(event)
//      }
//    }
//  }.also {
//    it.invokeOnCompletion {
//      scope.launch {
//        this@Producer.close()
//      }
//    }
//  }

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

    /**
     * 这个锁用来确保消息的执行，同一时间有且只能有一个消费者在消费
     */
    private val emitLock = Mutex()
    internal val emitJobs = SafeLinkList<Job>()

    /**
     * 消费事件
     *
     * 默认情况下，事件会被缓存，直到被消费
     * 但是这并不会停止向当前已有的其它消费器继续传播
     */
    fun consume(): T {
      if (!consumed) {
        consumed = true
      }
      return data
    }

    var stoped = false
      private set

    /**
     * 停止传播
     *
     * 事件消费，并停止向其它消费器继续传播
     */
    fun stopImmediatePropagation() {
      consume()
      stoped = true
    }

    /**
     * 在 complete 的时候再进行 remove，否则 buffers.joinAll() 的行为就不正确了
     */
    internal fun complete() {
      eventJob.complete()
      buffers.remove(this)
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

    inline fun <R : Any?> consumeMapNotNull(mapNotNull: (T) -> R?): R? {
      val result = mapNotNull(data)
      if (result != null) {
        consume()
        return result
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
        if (stoped) {
          return
        }

        traceTimeout(1000, { "consumer=$consumer data=$data" }) {
          // 对方的接收是非阻塞的，所以我们才会有 collectorLock，等待 consumer 挂载完成所有的 emitJobs
          consumer.collectorLock.lock()
          consumer.input.send(this)

          // 等待事件执行完成在往下走
          consumer.collectorLock.withLock {
            emitJobs.joinAll()
            emitJobs.clear()
          }
        }
        if (consumed) {
          complete()
          debugProducer("emitBy", "consumer=$consumer consumed data=$data")
        }
      }
    }
  }

  private val orderInvoker = OrderInvoker()

  private fun ensureOpen() {
    if (isClosedForSend) {
      throw Exception("$this already close for emit.")
    }
  }

  suspend fun send(value: T, order: Int? = null) = actionQueue.queue("send=$value") {
    ensureOpen()
    doSend(value, order)
  }

  private fun doSend(value: T, order: Int?) {
    val event = Event(value, order)
    buffers.add(event)
    if (buffers.size > 10) {
      WARNING("$this buffers overflow maybe leak: $buffers")
    }
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      doEmit(event)
    }
  }

  suspend fun sendBeacon(value: T, order: Int? = null) = actionQueue.queue("sendBeacon") {
    doSendBeacon(value, order)
  }

  private suspend fun doSendBeacon(value: T, order: Int?) {
    val event = Event(value, order)
    doEmit(event)
    when {
      event.consumed -> debugProducer("sendBeacon", event)
      else -> debugProducer("lostBeacon", event)
    }
  }

  suspend fun trySend(value: T, order: Int? = null) = actionQueue.queue("trySend") {
    if (isClosedForSend) {
      doSendBeacon(value, order)
    } else {
      doSend(value, order)
    }
  }


  private suspend fun doEmit(event: Event) {
    event.orderInvoke {
      withScope(scope) {
        for (consumer in consumers.toList()) {
          if (!consumer.started || consumer.startingBuffers?.contains(event) == true) {
            continue
          }
          event.emitBy(consumer)
          if (event.stoped) {
            break
          }
        }
      }
    }
  }

  /**创建消费者*/
  fun consumer(name: String): Consumer {
    ensureOpen()
    return Consumer(name)
  }

  inner class Consumer internal constructor(
    val name: String,
//    internal val input: MutableSharedFlow<Event> = MutableSharedFlow(),
    internal val input: Channel<Event> = Channel(),
  ) : Flow<Event> {
    val debugConsumer by lazy { Debugger(this.toString()) }
    override fun toString(): String {
      return "Consumer<[$producerName]$name>"
    }

    val producerName get() = this@Producer.name

    var started = false
      private set

    internal var startingBuffers: List<Event>? = null

    private val collectors = SafeLinkList<FlowCollector<Event>>()
    internal val collectorLock = Mutex()
    private val startCollect = SuspendOnce {
      val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        debugProducer("startCollect") {
          Exception().stackTraceToString().split("\n").firstOrNull {
            it.trim().run {
              startsWith("at org.dweb_browser") && !(startsWith("at org.dweb_browser.helper"))
            }
          }
        }
        for (event in input) {
          // 同一个事件的处理，不做任何阻塞，直接发出
          // 这里包一层launch，目的是确保不阻塞input的循环，从而确保上游event能快速涌入
          for (collector in collectors) {
            event.emitJobs += launch(start = CoroutineStart.UNDISPATCHED) {
              collector.emit(event)
            }
          }
          collectorLock.unlock()
        }
      }
      withScope(scope) {
        started = true
        val starting = buffers.toList()
        startingBuffers = starting
        /// 将之前没有被消费的逐个触发，这里不用担心 buffers 被中途追加，emit会同步触发
        for (event in starting) {
          launch(start = CoroutineStart.UNDISPATCHED) {
            event.orderInvoke {
              event.emitBy(this@Consumer)
            }
          }
        }
        startingBuffers = null
      }
      job.join()
    }

    override suspend fun collect(collector: FlowCollector<Event>) {
      collectors.add(collector)
      // 事件在收集了再调用开始
      startCollect()
    }

    init {
      consumers.add(this)
      invokeOnClose {
        cancel()
      }
    }

    fun cancel() {
      startCollect.reset()
      consumers.remove(this)
    }
  }

  var isClosedForSend = false
    private set


  /**
   * 首先会关闭写入，然后会将现有的events的调用末尾增加一个自消费
   */
  suspend fun close(cause: Throwable? = null) = actionQueue.queue("close") {
    if (isClosedForSend) {
      return@queue
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
            event.complete()
            debugProducer("closeWrite", "event=$event consumed by close")
          }
        }
      }
    }

    traceTimeout(1000, { this@Producer }) {
      // 等待消费者全部完成
      buffers.toList().joinAll()
    }

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

  /**调用监听关闭*/
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
  }
}

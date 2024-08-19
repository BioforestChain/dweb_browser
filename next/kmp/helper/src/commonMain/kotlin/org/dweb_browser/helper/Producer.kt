package org.dweb_browser.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * 消息生产者，彻底的消费掉消息需要显示调用consume()
 */
public class Producer<T>(public val name: String, parentContext: CoroutineContext) {
  public constructor(name: String, parentScope: CoroutineScope) : this(
    name,
    parentScope.coroutineContext
  )

  private val job = SupervisorJob()
  public val coroutineContext: CoroutineContext = parentContext + job
  public val scope: CoroutineScope by lazy { CoroutineScope(coroutineContext) }
  public val debugProducer: Debugger by lazy { Debugger(this.toString()) }
  public suspend fun join(): Unit = job.join()

  init {
    parentContext.job.invokeOnCompletion {
      debugProducer.verbose("parent closed")
      this@Producer.close(it)
    }
  }


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
  public inner class Event(
    public val data: T,
    order: Int?,
    private val eventJob: CompletableJob = Job(),
  ) :
    OrderBy, Job by eventJob {
    override val order: Int? = when {
      order == null && data is OrderBy -> data.order
      else -> order
    }
    private var consumeTimes = SafeInt(0)
    public val consumed: Boolean get() = consumeTimes.value > 0

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
    public fun consume(): T {
      consumeTimes++
      return data
    }

    public var stoped: Boolean = false
      private set

    /**
     * 停止传播
     *
     * 事件消费，并停止向其它消费器继续传播
     */
    public fun stopImmediatePropagation() {
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
    public inline fun <reified R : T> consumeAs(): R? {
      if (R::class.isInstance(data)) {
        return consume() as R
      }
      return null
    }

    public inline fun consumeFilter(filter: (T) -> Boolean): T? {
      if (filter(data)) {
        return consume()
      }
      return null
    }

    public inline fun <R> consumeMapNotNull(mapNotNull: (T) -> R?): R? {
      val result = mapNotNull(data)
      if (result != null) {
        consume()
        return result
      }
      return null
    }

    /**将其消耗转换为R对象 以回调形式继续传递*/
    public inline fun <reified R : T> consumeAs(block: (R) -> Unit) {
      if (R::class.isInstance(data)) {
        block(consume() as R)
      }
    }

    override fun toString(): String {
      return "Event($data)"
    }

    /**按顺序触发事件*/
    internal suspend inline fun orderInvoke(key: String, crossinline invoker: suspend () -> Unit) {
      orderInvoker.tryInvoke(order, key = "$this >> $key") {
        invoker()
      }
    }

    internal suspend fun emitBy(consumer: Consumer) {
      emitLock.withLock {
        if (stoped) {
          return
        }
        val beforeConsumeTimes = consumeTimes.value

        debugProducer.timeout(2000, "emitBy timeout", { "consumer=$consumer data=$data" }) {

          /// 这里使用channel将event和lock发送过去，但是 emit 返回只代表了对面接收到，不代表对面处理完
          /// 所以这里我们还需要等待对面处理完成，这里 emit(null) 就是这样一个等待作用，它可以确保上一个event被接受处理
          consumer.input.emit(this)
          consumer.input.emit(null)

          // 等待事件执行完成在往下走
          emitJobs.joinAll()
          emitJobs.clear()
        }
        if (consumed) {
          complete()
          if (consumeTimes.value != beforeConsumeTimes) {
            debugProducer.verbose("emitBy", "consumer=$consumer consumed data=$data")
          }
        }
      }
    }
  }

  private val orderInvoker = OrderInvoker()

  private fun ensureOpen(reason: String) {
    if (isClosedForSend) {
      throw Exception("$this fail to '$reason', already close for emit.")
    }
  }

  public suspend fun send(value: T, order: Int? = null): Unit =
    actionQueue.queueAndAwait("send=$value") {
      ensureOpen("send=$value")
      doSend(value, order)
    }

  private val warn = Once {
    WARNING(
      "$this buffers overflow maybe leak: (${buffers.size})/${
        when {
          buffers.size > 8 -> "${buffers.slice(0..<3)}...${buffers.slice((buffers.size - 3)..<buffers.size)}"
          else -> buffers.toString()
        }
      }"
    )
  }

  public var warningThreshold: Int = 10
  private fun doSend(value: T, order: Int?) {
    val event = Event(value, order)
    val consumers = this.consumers.toList()
    buffers.add(event)
    if (buffers.size > warningThreshold) {
      warn()
    }
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      doEmit(event, consumers)
    }
  }

  public suspend fun sendBeacon(value: T, order: Int? = null): Unit =
    actionQueue.queueAndAwait("sendBeacon") {
      doSendBeacon(value, order)
    }

  private suspend fun doSendBeacon(value: T, order: Int?) {
    val event = Event(value, order)
    doEmit(event)
    when {
      event.consumed -> debugProducer.verbose("sendBeacon", event)
      else -> debugProducer.verbose("lostBeacon", event)
    }
  }

  public suspend fun trySend(value: T, order: Int? = null): Unit =
    actionQueue.queueAndAwait("trySend") {
      if (isClosedForSend) {
        doSendBeacon(value, order)
      } else {
        doSend(value, order)
      }
    }


  private suspend fun doEmit(event: Event, consumers: List<Consumer> = this.consumers.toList()) {
    event.orderInvoke("doEmit") {
      withContext(coroutineContext) {
        for (consumer in consumers) {
          // 如果还没有start，则直接跳过
          if (!consumer.started) {
            continue
          }
          // 如果已经正在start中，并且当前事件正好被start处理着，则跳过
          if (consumer.startingBuffers?.contains(event) == true) {
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
  public fun consumer(name: String): Consumer {
    ensureOpen("consumer($name)")
    return Consumer(name)
  }

  public inner class Consumer internal constructor(
    public val name: String,
  ) : Flow<Event> {
    internal val input: MutableSharedFlow<Event?> = MutableSharedFlow()
    public val debugConsumer: Debugger by lazy { Debugger(this.toString()) }
    override fun toString(): String {
      return "Consumer<[$producerName]$name>"
    }

    public val producerName: String get() = this@Producer.name

    public var started: Boolean = false
      private set

    internal var startingBuffers: List<Event>? = null

    private val errorCatcher = CompletableDeferred<Throwable?>()
    private val collectOnce = SuspendOnce1(before = {
      if (this.haveRun) {
        throw Exception("$this was collected")
      }
    }) { collector: FlowCollector<Event> ->
      val deferred = scope.async(
        SupervisorJob(), start = CoroutineStart.UNDISPATCHED
      ) {
        debugProducer.verbose("startCollect") {
          Exception().stackTraceToString().split("\n").firstOrNull {
            it.trim().run {
              startsWith("at org.dweb_browser") && !(startsWith("at org.dweb_browser.helper"))
            }
          }
        }

        val job = input.filterNotNull().collectIn(scope) { event ->
          // 同一个事件的处理，不做任何阻塞，直接发出
          // 这里包一层launch，目的是确保不阻塞input的循环，从而确保上游event能快速涌入
          event.emitJobs += launch(start = CoroutineStart.UNDISPATCHED) {
            try {
              collector.emit(event)
            } catch (e: Throwable) {
              errorCatcher.complete(e)
            }
          }
        }
        val error = select<Throwable?> {
          job.onJoin { null }
          errorCatcher.onAwait { it }
        }

        job.cancel("collector emit error", error)

      }
      actionQueue.queueAndAwait("add-consumer") {
        consumers.add(this@Consumer)
        started = true
        val starting = buffers.toList()
        startingBuffers = starting
        /// 将之前没有被消费的逐个触发，这里不用担心 buffers 被中途追加，emit会同步触发
        for (event in starting) {
          launch(start = CoroutineStart.UNDISPATCHED) {
            event.orderInvoke("staring consumer=${this@Consumer}") {
              event.emitBy(this@Consumer)
            }
          }
        }
        startingBuffers = null
      }
      val x = invokeOnClose {
        deferred.cancel(CancellationException("${this@Producer} closed", it))
        consumers.remove(this@Consumer)
      }
      deferred.invokeOnCompletion {
        x.dispose()
        consumers.remove(this@Consumer)
      }
      deferred.await()
    }


    override suspend fun collect(collector: FlowCollector<Event>) {
      collectOnce(collector)
    }

    public suspend fun close(cause: Throwable? = null) {
      errorCatcher.complete(cause)
    }
  }

  public var isClosedForSend: Boolean = false
    private set


  public fun close(cause: Throwable? = null) {
    @Suppress("DeferredResultUnused") doClose(cause)
  }

  /**
   * 关闭 Producer
   * 首先会关闭写入，然后会将现有的events的调用末尾增加一个自消费
   * 注意，这个函数会自己在后台进行关闭，不会等待关闭后才返回
   * 如果有需要，请使用 closeAndAwait
   */
  private val doClose = Once1 { cause: Throwable? ->
    actionQueue.queue(scope, "close") {
      if (isClosedForSend) {
        return@queue
      }
      debugProducer.verbose("closeWrite", cause)
      isClosedForSend = true
      val bufferEvents = buffers.toList()
      debugProducer.verbose("closeEvents", bufferEvents)
      coroutineScope {
        for (event in bufferEvents) {
          launch(start = CoroutineStart.UNDISPATCHED) {
            event.orderInvoke("close") {
              if (!event.consumed) {
                event.consume()
                event.complete()
                debugProducer.verbose("closeWrite", "event=$event consumed by close")
              }
            }
          }
        }
      }


      debugProducer.timeout(1000,
        "close",
        { "closing and joinAll events=[${buffers.toList().joinToString(", ")}]" }) {
        // 等待消费者全部完成
        buffers.toList().joinAll()
      }

      debugProducer.verbose("close", "close consumers")
      // 关闭消费者channel，表示彻底无法再发数据
      for (consumer in consumers) {
        consumer.close(cause)
      }
      job.cancelOrThrow(cause)
      debugProducer.verbose("close", "free memory")
      consumers.clear()
      buffers.clear()
    }
  }

  public suspend fun closeAndJoin(cause: Throwable? = null) {
    runCatching {// 这里会抛出 job was cancelled 的异常
      doClose(cause).await()
    }
  }

  /**调用监听关闭*/
  public fun invokeOnClose(handler: CompletionHandler): DisposableHandle =
    job.invokeOnCompletion(handler)
}

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


class Producer<T>(val name: String, parentScope: CoroutineScope) {
  private val job = SupervisorJob(parentScope.coroutineContext[Job])
  val scope = parentScope + job
  val debugProducer by lazy { Debugger(this.toString()) }
  val await = job::await

  private val consumers = SafeHashSet<Consumer>()
  private val buffers = SafeLinkList<Event>()
  private val eventChannel = Channel<Event>(capacity = Channel.BUFFERED)

  override fun toString(): String {
    return "Producer<$name>"
  }

  inner class Event(val data: T, order: Int?, private val eventJob: CompletableJob = Job()) :
    OrderBy, Job by eventJob {
    override val orderBy = when {
      order == null && data is OrderBy -> data.orderBy
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

    inline fun <reified R : T> consumeAs(block: (R) -> Unit) {
      if (R::class.isInstance(data)) {
        block(consume() as R)
      }
    }

    override fun toString(): String {
      return "Event($data)"
    }


    internal suspend inline fun orderInvoke(crossinline invoker: suspend () -> Unit) {
      orderInvoker.tryInvoke(orderBy) {
        invoker()
      }
    }


    internal suspend fun emitBy(consumer: Consumer) {
      emitLock.withLock {
        if (consumed) {
          return@withLock
        }
        val job = scope.launch {
          delay(1000)
          WARNING("emitBy TIMEOUT!! consumer=$consumer data=$data")
        }
        emitJobsLock.lock()
        consumer.input.send(this)
        emitJobsLock.withLock {
          emitJobs.joinAll()
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
  fun ensureOpen() {
    if (eventChannel.isClosedForSend) {
      throw Exception("$this already close for emit.")
    }
  }

  suspend fun emit(value: T, order: Int? = null) {
    ensureOpen()
    val event = Event(value, order)
    buffers.add(event)
    if (buffers.size > 10) {
      WARNING("$this buffers overflow maybe leak: $buffers")
    }
    eventChannel.send(event)
//    event.join()
//    doEmit(event)
  }

  suspend fun sendBeacon(value: T, order: Int? = null) {
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

    @OptIn(DelicateCoroutinesApi::class)
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
            collector.emit(event)
          }
          event.emitJobsLock.unlock()
        }
      }
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

  var isCloseForEmit = false
    private set

  fun closeWrite(cause: Throwable? = null) {
    isCloseForEmit = true
    for (event in buffers.toList()) {
      scope.launch(start = CoroutineStart.UNDISPATCHED) {
        event.orderInvoke {
          if (!event.consumed) {
            event.consume()
            debugProducer("closeWrite", "event=$event consumed by close")
          }
        }
      }
    }
    eventChannel.close(cause)
  }

  fun close(cause: Throwable? = null) {
    closeWrite(cause)

  }

  init {
    scope.launch {
      for (event in eventChannel) {
        launch(start = CoroutineStart.UNDISPATCHED) {
          doEmit(event)
        }
      }
      buffers.toList().joinAll()

      debugProducer("close")
      for (consumer in consumers) {
        consumer.input.close()
      }
      scope.close()
    }

    job.invokeOnCompletion {
      debugProducer("invokeOnCompletion", "free memory")
      consumers.clear()
      buffers.clear()
    }
  }

  fun invokeOnClose(handler: CompletionHandler) {
    scope.coroutineContext[Job]!!.invokeOnCompletion(handler)
  }
}

fun <T> Flow<T>.asProducer(
  name: String,
  scope: CoroutineScope,
  emitter: suspend Producer<T>.(T) -> Unit = { emit(it) },
) = Producer<T>(name, scope).also { producer ->
  collectIn(producer.scope) {
    producer.emitter(it)
  }.invokeOnCompletion {
    producer.close()
  }
}
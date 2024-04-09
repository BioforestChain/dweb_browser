package org.dweb_browser.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class Producer<T>(val name: String, parentScope: CoroutineScope) {
  private val job = SupervisorJob(parentScope.coroutineContext[Job])
  val scope = parentScope + job
  val debugProducer by lazy { Debugger(this.toString()) }

  private val consumers = SafeHashSet<Consumer>()
  private val buffers = SafeLinkList<Event>()

  override fun toString(): String {
    return "Producer<$name>"
  }

  inner class Event(val data: T) {
    var consumed = false
      private set
    private val emitLock = Mutex()
    fun consume(): T {
      if (!consumed) {
        consumed = true
        buffers.remove(this)
      }
      return data
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

    internal suspend fun emitBy(consumer: Consumer) {
      emitLock.withLock {
        if (consumed) {
          return@withLock
        }
        consumer.input.emit(this)
        if (consumed) {
          debugProducer("emitBy", "consumer=$consumer consumed data=$data")
        }
      }
    }
  }

  suspend fun emit(value: T) {
    withScope(scope) {
      val event = Event(value)
      buffers.add(event)
      if (buffers.size > 10) {
        WARNING("$this buffers overflow maybe leak: $buffers")
      }
      for (consumer in consumers) {
        if (!consumer.started) {
          continue
        }
        event.emitBy(consumer)
        if (event.consumed) {
          break
        }
      }
    }
  }

  fun consumer(name: String) = Consumer(name)

  inner class Consumer internal constructor(
    val name: String,
    internal val input: MutableSharedFlow<Event> = MutableSharedFlow(),
  ) : Flow<Event> by input {
    val debugConsumer by lazy { Debugger(this.toString()) }
    override fun toString(): String {
      return "Consumer<[$producerName]$name>"
    }

    val producerName get() = this@Producer.name

    var started = false
      private set

    private val start = SuspendOnce {
      withScope(scope) {
        started = true
        debugConsumer("startJob", "begin $buffers")
        /// 将之前没有被消费的逐个触发，这里不用担心 buffers 被中途追加，emit会同步触发
        for (event in buffers.toList()) {
          event.emitBy(this@Consumer)
        }
        debugConsumer("startJob", "done $buffers")
      }
    }

    override suspend fun collect(collector: FlowCollector<Event>) {
      scope.launch(start = CoroutineStart.UNDISPATCHED) {
        input.collect(collector)
      }
      withScope(scope) {
        start()
      }
    }

    init {
      consumers.add(this)
    }

    fun cancel() {
      start.reset()
      consumers.remove(this)
    }
  }

  fun close(cause: CancellationException? = null) {
    scope.cancel(cause)
  }

  init {
    job.invokeOnCompletion {
      consumers.clear()
      buffers.clear()
    }
  }

  fun invokeOnClose(handler: CompletionHandler) {
    scope.coroutineContext[Job]!!.invokeOnCompletion(handler)
  }
}

fun <T> Flow<T>.asProducer(name: String, scope: CoroutineScope) =
  Producer<T>(name, scope).also { producer ->
    collectIn(producer.scope) {
      producer.emit(it)
    }
  }
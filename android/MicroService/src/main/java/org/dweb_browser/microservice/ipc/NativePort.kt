package org.dweb_browser.microservice.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.util.concurrent.atomic.AtomicInteger

class NativeMessageChannel<T1, T2> {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val closePo = PromiseOut<Int>()
  private val channel1 = Channel<T1>()
  private val channel2 = Channel<T2>()
  val port1 = NativePort(channel1, channel2, closePo)
  val port2 = NativePort(channel2, channel1, closePo)
}

class NativePort<I, O>(
  private val channel_in: Channel<I>,
  private val channel_out: Channel<O>,
  private val closePo: PromiseOut<Int>
) {
  companion object {
    private var uid_acc = AtomicInteger(1);
  }

  private val uid = uid_acc.getAndAdd(1)
  override fun toString() = "#p$uid"

  private var started = false
  suspend fun start() {
    if (started) return else started = true
    /**
     * 等待 close 信号被发出，那么就关闭出口、触发事件
     */
    if (closePo.isFinished) {
      _close()
    } else {
      CoroutineScope(currentCoroutineContext()).launch {
        if (closePo.waitPromise() != uid) {
          _close()
        }
      }
    }

    debugNativeIpc("port-message-start/$this")
    for (message in channel_in) {
      debugNativeIpc("port-message-in/$this << $message")
      _messageSignal.emit(message)
      debugNativeIpc("port-message-waiting/$this")
    }
    debugNativeIpc("port-message-end/$this")
  }

  private val _closeSignal = SimpleSignal()

  fun onClose(cb: SimpleCallback) = _closeSignal.listen(cb)

  suspend fun close() {
    if (!closePo.isFinished) {
      closePo.resolve(uid)
      _close()
    }
  }

  private suspend fun _close() {
    /// 关闭输出就行了
    channel_out.close()
    _closeSignal.emitAndClear()
    debugNativeIpc("port-closed/${this}")
  }

  private val _messageSignal = Signal<I>()

  /**
   * 发送消息，这个默认会阻塞
   */
  @OptIn(DelicateCoroutinesApi::class)
  suspend fun postMessage(msg: O) {
    debugNativeIpc("message-out/$this >> $msg")
    if (!channel_out.isClosedForSend) {
      channel_out.send(msg)
    } else {
      debugNativeIpc("postMessage", " handle the closed channel case!")
    }
  }

  /**
   * 监听消息
   */
  fun onMessage(cb: Callback<I>) = _messageSignal.listen(cb)
}
package org.dweb_browser.core.ipc

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal

class NativeMessageChannel<T1, T2>(fromId: MMID, toId: MMID) {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val closePo = SharedCloseSignal()
  private val channel1 = Channel<T1>()
  private val channel2 = Channel<T2>()
  val port1 = NativePort(channel1, channel2, closePo, "<$fromId=>$toId>")
  val port2 = NativePort(channel2, channel1, closePo, "<$toId=>$fromId>")
}

class SharedCloseSignal {
  private val closeSignal = SimpleSignal()
  fun onClose(cb: Callback<Unit>) = closeSignal.listen(cb)
  private var closed = false
  private val _lock = SynchronizedObject()
  fun isClosed(): Boolean {
    synchronized(_lock) {
      return closed
    }
  }

  suspend fun emitClose() {
    synchronized(_lock) {
      if (closed) {
        return
      }
      closed = true
    }
    closeSignal.emitAndClear()
  }
}

class NativePort<I, O>(
  private val channelIn: Channel<I>,
  private val channelOut: Channel<O>,
  private val cs: SharedCloseSignal,
  private val descriptor: String,
) {
  companion object {
    private var uid_acc by SafeInt(1);
  }

  private val uid = uid_acc++
  override fun toString() = "NativePort@$uid#$descriptor"

  private var started = false
  suspend fun start() {
    if (started) return else started = true
    /**
     * 等待 close 信号被发出，那么就关闭出口、触发事件
     */
    if (cs.isClosed()) {
      _close()
    } else {
      cs.onClose {
        _close()
      }
    }

    debugNativeIpc("port-message-start", "$this")
    for (message in channelIn) {
      debugNativeIpc("port-message-in", "$this << $message")
      _messageSignal.emit(message)
      debugNativeIpc("port-message-waiting", "$this")
    }
    debugNativeIpc("port-message-end", "$this")
  }

  private val _closeSignal = SimpleSignal()

  fun onClose(cb: SimpleCallback) = _closeSignal.listen(cb)

  suspend fun close() {
    return cs.emitClose()
  }

  private suspend fun _close() {
    /// 关闭输出就行了
    channelOut.close()
    _closeSignal.emitAndClear()
    debugNativeIpc("port-closed", "$this")
  }

  private val _messageSignal = Signal<I>()

  /**
   * 发送消息，这个默认会阻塞
   */
  @OptIn(DelicateCoroutinesApi::class)
  suspend fun postMessage(msg: O) {
    debugNativeIpc("message-out", "$this >> $msg >> ${!channelOut.isClosedForSend}")
    if (!channelOut.isClosedForSend) {
      channelOut.send(msg)
    } else {
      debugNativeIpc("postMessage", " handle the closed channel case!")
    }
  }

  /**
   * 监听消息
   */
  fun onMessage(cb: Callback<I>) = _messageSignal.listen(cb)
}
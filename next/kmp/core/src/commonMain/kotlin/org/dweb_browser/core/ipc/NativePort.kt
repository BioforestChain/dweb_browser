package org.dweb_browser.core.ipc

import kotlinx.coroutines.flow.MutableSharedFlow
import org.dweb_browser.core.help.types.MMID

class NativeMessageChannel<T1, T2>(fromId: MMID, toId: MMID) {
  /**
   * 默认锁住，当它解锁的时候，意味着通道关闭
   */
  private val flow1 = MutableSharedFlow<T1>()
  private val flow2 = MutableSharedFlow<T2>()
  val port1 = NativePort(flow1, flow2, "<$fromId=>$toId>")
  val port2 = NativePort(flow2, flow1, "<$toId=>$fromId>")
}

class NativePort<I, O>(
  flowIn: MutableSharedFlow<I>,
  private val flowOut: MutableSharedFlow<O>,
  private val descriptor: String,
) {
  override fun toString() = "NativePort#$descriptor"

  /**
   * 监听消息
   */
  val onMessage = flowIn

  /**
   * 发送消息，这个默认会阻塞
   */
  suspend fun postMessage(msg: O) {
    debugNativeIpc("message-out", "$this >> $msg ")
    flowOut.emit(msg)
  }
}
package org.dweb_browser.core.ipc.helper

import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.base64String
import org.dweb_browser.pure.http.IPureBody


val debugIpcBody = Debugger("ipc-body")

abstract class IpcBody {
  /**
   * 缓存，这里不提供辅助函数，只是一个统一的存取地方，
   * 写入缓存者要自己维护缓存释放的逻辑
   */
  class CACHE {
    companion object {

      /**
       * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
       * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
       */
      val streamId_receiverIpc_Map = mutableMapOf<String/*streamId*/, Ipc>()

      /**
       * 每一个 metaBody 背后，都会有一个 IpcBodySender,
       * 这里主要是存储 流，因为它有明确的 open/close 生命周期
       */
      val streamId_ipcBodySender_Map = mutableMapOf<String, IpcBodySender>()
    }

  }


  abstract val raw: IPureBody
  abstract val metaBody: MetaBody

  fun stream() = raw.toPureStream()

  suspend fun base64() = raw.toPureBinary().base64String

  suspend fun text() = raw.toPureString()

}

package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy

@Serializable
@SerialName(IPC_MESSAGE_TYPE_STREAM_PULL)
data class IpcStreamPulling(
  override val stream_id: String,
  /**
   * 带宽限制, 0 代表不限速。
   * 负数代表暂停，但对于数据暂停，一般使用 Paused 指令。
   * 如果出现负数，往往是代表对方的数据处理出现了阻塞，与 Paused 不同，Paused 代表的是逻辑上的暂停，可能是挂起去处理其它事情去了，
   * 而负数的带宽代表物理意义上的阻塞，此时更不该再发送更多的数据过去
   */
  val bandwidth: Int = 0,
  override val order: Int = stream_id.hashCode(),
) : IpcRawMessage, IpcMessage, IpcStream, OrderBy
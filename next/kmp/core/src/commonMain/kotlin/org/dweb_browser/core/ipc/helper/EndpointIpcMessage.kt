package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy


/**分发消息到各个ipc的监听时使用
 *
 * 这里的 orderBy 和 IpcMessage 里头的 orderBy 不冲突，是两层各自去排序
 */
@Serializable
data class EndpointIpcMessage(
  val pid: Int,
  val ipcMessage: IpcMessage,
) : EndpointMessage(ENDPOINT_MESSAGE_TYPE.IPC)
package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**分发消息到各个ipc的监听时使用*/
@Serializable
data class EndpointMessage(val pid: Int, val ipcMessage: IpcMessage)
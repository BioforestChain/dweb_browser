package org.dweb_browser.js_common.network.socket

import kotlinx.serialization.Serializable
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer

@Serializable
data class SocketData(
    /**
     * 用来区分不同的ViewModel
     */
//    @JsName("id") val id: String,
    /**
     * path 用来描述 ViewModelState 的key路径
     * 例如：
     * ViewModelState = { a:{b: ComposeFlow}}
     * path = "a/b"
     */
//    @JsName("path") val path: String,

    /**
     * ComposeFlow 的标识符
     */
    @JsName("composeFlowId") val composeFlowId: String,
    /**
     * data 是一个序列化后的字符串
     */
    @JsName("data") val data: String,
)
package org.dweb_browser.js_common.network.socket

import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer


interface ISocketData<T: Any>{
    /**
     * 用来区分不同的ViewModel
     */
    val id: String
    /**
     * path 用来描述 ViewModelState 的key路径
     * 例如：
     * ViewModelState = { a:{b: ComposeFlow}}
     * path = "/a/b"
     */
    val path: String
    val composeFlowId: String
    val data: OperationValueContainer<T>
}
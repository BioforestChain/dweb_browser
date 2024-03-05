package org.dweb_browser.js_common.state_compose.operation

import org.dweb_browser.js_common.state_compose.state.EmitType

/**
 * @property index {Int} 索引
 * @property value {T} 传递给
 * @property emitType { EmitType } 发送消息的类型
 */
interface IOperationValueContainer<T: Any>{
    @JsName("index")
    val index: Int
    @JsName("value")
    val value: T
    @JsName("emitType")
    val emitType: EmitType
}

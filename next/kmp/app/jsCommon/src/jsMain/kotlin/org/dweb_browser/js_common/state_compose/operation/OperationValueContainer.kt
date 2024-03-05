package org.dweb_browser.js_common.state_compose.operation

import kotlinx.serialization.Serializable
import org.dweb_browser.js_common.state_compose.state.EmitType

@Serializable
data class OperationValueContainer<T: Any>(
    @JsName("index")
    val index: Int,
    @JsName("value")
    val value: T,
    @JsName("emitType")
    val emitType: EmitType
)


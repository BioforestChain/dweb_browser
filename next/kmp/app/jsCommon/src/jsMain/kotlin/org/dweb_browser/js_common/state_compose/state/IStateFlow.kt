package org.dweb_browser.js_common.state_compose.state
import kotlinx.coroutines.flow.MutableSharedFlow
import org.dweb_browser.js_common.state_compose.operation.OperationValueContainer
import org.dweb_browser.js_common.state_compose.serialization.Serialization


/**
 * 状态流
 * - 提供解码和编码的方法
 * - 发送数据的方法
 * - 保留完整数据的方法
 */
interface IStateFlow<T: Any>{
    // 用来保存完整数据的flow
    val stateFlow: MutableSharedFlow<T>
}





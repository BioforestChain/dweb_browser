package org.dweb_browser.js_frontend.state_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.state_compose.ComposeFlow
import kotlin.reflect.KProperty


@Composable
fun <ItemType: Any, ValueType: ItemType, CloseReason: Any> ComposeFlow.StateComposeFlow<ItemType, ValueType, CloseReason>.toMutableStateOf(
    initValue: ValueType
): MutableState<ItemType, ValueType, CloseReason> {
    return remember {
        MutableState(initValue, this)
    }
}

class MutableState<ItemType: Any, ValueType: ItemType, CloseReason: Any>(
    initValue: ValueType,
    val stateCompose: ComposeFlow.StateComposeFlow<ItemType, ValueType, CloseReason>,
){
    val mutableState = mutableStateOf(initValue)

    var value: ValueType
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }
    fun component1() = mutableState.component1()
    fun component2() = mutableState.component2()
    operator fun getValue(thisObj: Any?, property: KProperty<*>): ValueType = value

    operator fun setValue(
        thisObj: Any?, property: KProperty<*>, value: ValueType
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            stateCompose.emitByClient(value, EmitType.REPLACE)
        }
    }

    init{
        CoroutineScope(Dispatchers.Default).launch {
            stateCompose.collectServer{
                this@MutableState.value = it
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            stateCompose.collectClient{
                this@MutableState.value = it
            }
        }
    }
}

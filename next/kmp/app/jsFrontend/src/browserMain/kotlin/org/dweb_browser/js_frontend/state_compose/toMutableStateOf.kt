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
fun <T: Any, CloseReason: Any> ComposeFlow.StateComposeFlow<T, CloseReason>.toMutableStateOf(
    initValue: T
): MutableState<T, CloseReason> {
    return remember {
        MutableState(initValue, this)
    }
}

class MutableState<T: Any, CloseReason: Any>(
    initValue: T,
    val stateCompose: ComposeFlow.StateComposeFlow<T, CloseReason>,
){
    val mutableState = mutableStateOf(initValue)

    var value: T
        get() = mutableState.value
        set(value) {
            mutableState.value = value
        }
    fun component1() = mutableState.component1()
    fun component2() = mutableState.component2()
    operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value

    operator fun setValue(
        thisObj: Any?, property: KProperty<*>, value: T
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

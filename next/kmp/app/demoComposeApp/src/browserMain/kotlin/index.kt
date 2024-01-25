import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState as IMutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.skiko.wasm.onWasmReady
import kotlin.reflect.KProperty
import viewModel.ViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 10))
    viewModel.start()

    onWasmReady {
        CanvasBasedWindow("Chat") {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    var count by viewModel.toMutableStateOf("currentCount")
//
                    Button(onClick = {
                        count = count+1
                    }) {
                        Text("increment count:$count")
                    }
                }
            }
        }
    }



//    class MutableState<T>(val initValue: T) : IMutableState<T> {
//
//        val mutableState = mutableStateOf<T>(initValue)
//        override var value: T
//            get() = mutableState.value
//            set(value) {
//                console.log("需要同步UI的数据到服务器")
//                // TODO: 需要同步UI的数据到服务器
//                mutableState.value = value
//            }
//
//        override fun component1() = mutableState.component1()
//        override fun component2() = mutableState.component2()
//        operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value
//        operator fun setValue(
//            thisObj: Any?, property: KProperty<*>, value: T
//        ) {
//            this.value = value
//        }
//
//    }
//
//    // TODO: 需要解决拦截问题
//    class MyState() {
//        val mutableStateMap = mutableMapOf<Any, MutableState<Int>>()
//        @Composable
//        fun toMutableStateOf(key: dynamic) = remember {
//            var mutableState = mutableStateMap[key]
//            if(mutableState == null){
//                mutableState = MutableState(0)
//                mutableStateMap[key] = mutableState
//            }
//            mutableState
//        }
//    }
//
//    val state = MyState()
//
//    onWasmReady {
//        CanvasBasedWindow("Chat") {
//            Column(modifier = Modifier.fillMaxSize()) {
//                Row(
//                    modifier = Modifier.fillMaxWidth().height(100.dp)
//                ) {
//
//                    var count by state.toMutableStateOf("key")
////                    val list = mutableStateListOf<Int>(1,2)
//                    Button(onClick = {
//                        count++
//                    }) {
//                        Text("increment count:$count")
//                    }
//                }
////
//                Row(
//                    modifier = Modifier.fillMaxWidth().height(100.dp)
//                ) {
//                    var count by state.toMutableStateOf("key")
//                    Button(
//                        onClick = {
//                            count++
//                        }
//                    ){
//                        Text("increment count:$count")
//                    }
//                }
//                val count = remember { mutableStateListOf<Int>(1,2) }
//                Row(
//                    modifier = Modifier.fillMaxWidth().height(100.dp)
//                ) {
//                    // TODO: 需要实现共享
//                    Button(
//                        onClick = {
//                            count.add(count.size)
//                        }
//                    ){
//                        Text("add")
//                    }
//                }
//
//                Row(
//                    modifier = Modifier.fillMaxWidth().height(100.dp)
//                ) {
//                    count.forEach {
//                        Text("it: $it")
//                    }
//
//                }
//            }
//        }
//    }
}

// 测试 mutalbeState 是否可以实现一对多








import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.view_model.DataState
import org.dweb_browser.js_common.view_model.DataStateValue
import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_frontend.state_compose.MutableState
import org.dweb_browser.js_frontend.state_compose.toMutableStateListOf
import org.jetbrains.skiko.wasm.onWasmReady
import org.dweb_browser.js_frontend.state_compose.toMutableStateOf


@Serializable
class Person(
    @JsName("name") val name: String, @JsName("id") val id: Int
)

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {

    // class 有额外的 p98_1:0这样的属性问题
    // 在 main() 里面声明的类不会有
    var personCount = 0

    val count = DataStateValue.createStateValue<Int, String>()
    val persons = DataStateValue.createListValue<Person, String>()
//    val sub = DataStateValue.createMapValue(
//        value = mapOf<String, DataStateValue<*>>(
//            "count" to DataStateValue.createStateValue<Int, String>(),
//            "persons" to DataStateValue.createListValue<Person, String>()
//        )
//    )

    val dataState: DataState = mapOf<String, DataStateValue<*>>(
        "count" to count,
        "persons" to persons,
//        "sub" to sub
    )

    val windowModule = ElectronBrowserWindowModule(
        VMId = "demo.compose.app", dataState = dataState
    ) {
        console.log(" 接受到了服务器发送过来的同步数据", it)
        when{
            it.path == "count" -> {
                val operationValueContainer = count.value.decodeFromString(it.data)
                CoroutineScope(Dispatchers.Default).launch{
                        count.value.emitByServer(operationValueContainer.value, operationValueContainer.emitType)
                }
            }

            it.path == "persons" -> {
                val operationValueContainer = persons.value.decodeFromString(it.data)
                CoroutineScope(Dispatchers.Default).launch {
                    if(operationValueContainer.index != -1){
                        persons.value.emitByServer(operationValueContainer.value, operationValueContainer.emitType)
                    }else{
                        persons.value.emitByServer(operationValueContainer.value, operationValueContainer.emitType, operationValueContainer.index)
                    }

                }
            }
            else -> {
                console.log(it)
                console.log(it.path.split("/"))
            }
        }
    }

    onWasmReady {
        CanvasBasedWindow("Chat") {
//            var a by ComposeFlow.createStateComposeFlowInstance<Int, String>().toMutableStateOf(1)
//            val b by (DataStateValue.StateFlow.createStateFlow<Int, String>().flow as ComposeFlow.StateComposeFlow).toMutableStateOf(1)
//            val c by (DataStateValue.createValueStateFlowInstance<Int, String>().flow as ComposeFlow.StateComposeFlow).toMutableStateOf(1)
//            val d by (count.flow as ComposeFlow.StateComposeFlow).toMutableStateOf(1)
            Column(modifier = Modifier.fillMaxSize()) {
                var insidePersons = persons.value.toMutableStateListOf()
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick={
                        window.location.reload()
                    }){
                        Text("reload")
                    }

                    var insideCount by count.value.toMutableStateOf(1)


                    Button(onClick = {
                        insideCount++
                        console.log("insideCount: ", insideCount)
                    }) {
                        Text("increment count:$insideCount")
                    }

                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick = {
                        personCount++;
                        CoroutineScope(Dispatchers.Default).launch {
                            insidePersons.add(Person("$personCount name", personCount))
                        }

                    }) {
                        Text("increment count: add")
                    }
                }

                insidePersons.forEach {
                    Row(){
                        Text("${it.id} - ${it.name}")
                    }
                }
            }
        }
    }
}
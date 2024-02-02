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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_common.view_model.SyncType

import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_frontend.view_model.ViewModelState
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {

    // class 有额外的 p98_1:0这样的属性问题
    // 在 main() 里面声明的类不会有

    val module = ElectronBrowserWindowModule(
        moduleId =  "js.backend.dweb",
        encodeValueToString = {key: String, value: dynamic, syncType: SyncType ->
            when{
                key == "currentCount" -> "$value"
                key == "persons" && syncType.value == SyncType.ADD.value -> Json.encodeToString<Person>(value)
                else -> throw(Throwable("""
                    encodeValueToString还没有没处理的
                    key: $key
                    value: $value
                    syncType: ${SyncType}
                    at decodeValueFromString
                    at index.kt
                    at demoComposeApp
                """.trimIndent()))
            }
        },
        decodeValueFromString = {key: String, value: String, syncType: SyncType ->
            when{
                key == "currentCount" -> value.toInt()
                key == "persons" && syncType.value == SyncType.REPLACE.value -> Json.decodeFromString<MutableList<Person>>(value)
                else -> console.error("""
                      decodeValueFromString还没有没处理的
                      key: $key
                      value: $value
                      syncType: ${SyncType}
                      at decodeValueFromString
                      at index.kt
                      at demoComposeApp
                """.trimIndent())
            }
        }
    )
    module.viewModel.whenSyncDataFromServerDone.await()
    onWasmReady {
        CanvasBasedWindow("Chat") {
            val list: ViewModelState.MutableStateList<Person> =
                module.viewModel.state.toMutableStateListOf("persons")
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    var count by module.viewModel.state.toMutableStateOf("currentCount")

                    Button(onClick = {
                        count = count + 1
                    }) {
                        Text("increment count:$count")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick = {
                        module.controller.close()
                    }) {
                        Text("increment count: close")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick = {
                        module.controller.reload()
                    }) {
                        Text("increment count: reload")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick = {
                        val p = Person(name = "name-${list.size}", id=list.size + 1)
                        list.add(p)
                    }) {
                        Text("list add")
                    }
                    Button(onClick = {
//                        val ps = arrayOf(Person(name = "name-1-${list.size}", id=list.size + 1), Person(name = "name-1-${list.size}", id=list.size + 1))
//                        list.addAll(ps)
                    }){
                        Text("list addAll")
                    }
                }

                list.forEach {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("name: ${it.name}")
                        Text("id: ${it.id}")
                    }
                }
            }
        }
    }
}



@Serializable
class Person(
    @JsName("name")
    val name: String,
    @JsName("id")
    val id: Int
)



//@Serializable
//data class Person(
//    @JsName("name") val name: String, @JsName("id") val id: Int
//)

// 用来测试实例化类没有额外的属性
//fun main(){
//    val bill = Person("bill", 1)
//    // 使用 kotlinx Serialization 可以避免额外的
//    val str = Json.encodeToString(bill)
//    val b = Json.decodeFromString<Person>(str)
//    console.log("str:", str)
//    console.log("b: ", b)
//    console.log(JSON.stringify(bill))
//
//    val list = listOf(bill)
//    console.log(Json.encodeToString(list))
//}

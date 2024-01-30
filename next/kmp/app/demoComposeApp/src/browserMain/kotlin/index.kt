import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow

import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_frontend.view_model_state.ViewModelState
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {
    // class 有额外的 p98_1:0这样的属性问题
    // 在 main() 里面声明的类不会有
    val module = ElectronBrowserWindowModule("js.backend.dweb")
    module.viewModel.dwebWebSocket.start()
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
                        val ps = listOf(Person(name = "name-1-${list.size}", id=list.size + 1), Person(name = "name-1-${list.size}", id=list.size + 1))
                        list.addAll(ps)
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

class Person(
    @JsName("name") val name: String, @JsName("id") val id: Int
)

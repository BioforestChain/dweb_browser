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
import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_frontend.state_compose.toMutableStateListOf
import org.jetbrains.skiko.wasm.onWasmReady
import org.dweb_browser.js_frontend.state_compose.toMutableStateOf


@Serializable
class Person(
    @JsName("name") val name: String, @JsName("id") val id: Int
)

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {
    var personCount = 0
    val count = ComposeFlow.createStateComposeFlowInstance<Int, Int, String>("count_id")
    val persons = ComposeFlow.createListComposeFlowInstance<Person, List<Person>,String>("persons_id")
    ElectronBrowserWindowModule(
        VMId = "demo.compose.app"
    ).apply {
        viewModelDataState.run{
            composeFlowListAdd(count)
            composeFlowListAdd(persons)
            socketStart()
        }
    }

    onWasmReady {
        CanvasBasedWindow("Chat") {
            Column(modifier = Modifier.fillMaxSize()) {
                val insidePersons = persons.toMutableStateListOf()
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Button(onClick={
                        window.location.reload()
                    }){
                        Text("reload before and after refreshing must same ")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().height(50.dp)){
                    var insideCount by count.toMutableStateOf(1)
                    Button(onClick = {
                        insideCount++
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
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
import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowModule
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {
    val module = ElectronBrowserWindowModule("js.backend.dweb")
//    val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 10))
//    val viewModel = ViewModel()
    module.viewModel.dwebWebSocket.start()
    module.viewModel.whenSyncDataFromServerDone.await()
    onWasmReady {
        CanvasBasedWindow("Chat") {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    var count by module.viewModel.state.toMutableStateOf("currentCount")
                    Button(onClick = {
                        count = count+1
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
            }
        }
    }
}

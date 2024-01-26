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
import org.jetbrains.skiko.wasm.onWasmReady
import viewModel.ViewModel

@OptIn(ExperimentalComposeUiApi::class)
suspend fun main() {
//    val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 10))
    val viewModel = ViewModel()
    viewModel.start()
    viewModel.whenSyncDataFromServerStart.await()
    onWasmReady {
        CanvasBasedWindow("Chat") {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    var count by viewModel.state.toMutableStateOf("currentCount")
                    Button(onClick = {
                        count = count+1
                    }) {
                        Text("increment count:$count")
                    }
                }
            }
        }
    }
}

// 测试 mutalbeState 是否可以实现一对多








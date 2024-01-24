

import js.promise.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import react.dom.client.createRoot
import web.dom.document
import react.*
import react.dom.*
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.button
import web.http.fetch
import viewModel.ViewModel


fun main(){
    val viewModel = ViewModel(mutableMapOf<String, dynamic>("currentCount" to 10))
    viewModel.start()
    val container = document.getElementById("root") ?: error("Couldn't find root container!")
    val root = createRoot(container)
    root.render(createApp(viewModel))
}

fun createApp(viewModel: ViewModel): ReactElement<PropsWithChildren>{
    return FC<Props>{ props ->
        var currentState by viewModel.toUseState<Int>("currentCount")

        h1 {
            + "标题1"
        }
        p {
            span{
                + "count:"
            }
            span {
                + "${currentState}"
            }
        }
        button{
            onClick = {
//        viewModel.set("count", 1)
                // TODO: 更新state
                currentState++
            }
            + "increment"
        }
        button{
            onClick = {
                CoroutineScope(Dispatchers.Default).launch{
                    val response = fetch("http://127.0.0.1:8888/demoReactApp/index.html")
                    console.log(response.text().await())
                }
            }
            + "测试请求html"
        }
        button{
            onClick = {
                viewModel.electronWindowOperation.close()
            }
            + "关闭window"
        }
    }.create()
}


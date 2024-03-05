package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowController
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_common.state_compose.state.EmitType
import org.dweb_browser.js_common.view_model.DataState
import org.dweb_browser.js_common.view_model.DataStateValue
import kotlin.js.Promise

@Serializable
class Person(
  @JsName("name")
  val name: String,
  @JsName("id")
  val id: Int
)

suspend fun main() {

  val count = DataStateValue.createStateValue<Int, String>()
  val persons = DataStateValue.createListValue<Person, String>()
//  val sub = DataStateValue.createMapValue(
//    value = mapOf<String, DataStateValue<*>>(
//      "count" to DataStateValue.createStateValue<Int, String>(),
//      "persons" to DataStateValue.createListValue<Person, String>()
//    )
//  )

  val dataState: DataState = mapOf<String, DataStateValue<*>>(
    "count" to count,
    "persons" to persons,
//    "sub" to sub
  )

  CoroutineScope(Dispatchers.Default).launch {
    count.value.collectServer{
      console.log("byServer")
    }
  }

  val job = Job()
  // 模拟数据生成在UI实例化之前的情况
  CoroutineScope(Dispatchers.Default).launch {
    count.value.emitByServer(10, emitType = EmitType.REPLACE)
    persons.value.emitByServer(listOf(Person("bill", 1)), emitType = EmitType.REPLACE)
//    sub.value.values.forEach {
//        val dataValue = it.value;
//        when(dataValue){
//          is ComposeFlow.StateComposeFlow<*, *> -> {
//            dataValue.emitByServer(1 as Nothing, emitType = EmitType.REPLACE)
//          }
//          is ComposeFlow.ListComposeFlow<*, *> -> {
//            dataValue.emitByServer(listOf(Person("bill-2", 2)) as List<Nothing>, EmitType.REPLACE)
//          }
//        }
//    }

    job.cancel()
  }

  job.join()

  val demoComposeApp = ElectronBrowserWindowModule(
    subDomain = "demo.compose.app",
    dataState = dataState
  ).apply {

//    count.value.collectOperationServer{
//      val socketData = SocketData(
//        id = subDomain,
//        path = "count",
//        data = count.value.encodeToString(it)
//      )
//      val jsonStr = Json.encodeToString(socketData)
//      console.log("jsonStr: ", jsonStr)
//    }

    // 测试初始化数据
    CoroutineScope(Dispatchers.Default).launch {
      delay(5000)
      count.value.emitByServer(100, EmitType.REPLACE)
      persons.value.emitByServer(listOf(Person(name = "bill-2", id = 2)), EmitType.ADD)
      console.log("emitByServer")
    }






//    viewModel.onUpdateByClient{key: String, value: dynamic, syncType ->
//      console.error("server received data from client key: value : syncType ", key, ":", value, ":", syncType)
////            viewModel[key] = value + 1
//    }
    controller.open(ElectronBrowserWindowController.createBrowserWindowOptions().apply {
      width = 1300.0
      height = 1000.0
    })
  }
}

fun <T> Promise<T>.toDeferred(): Deferred<T> {
  val deferred = CompletableDeferred<T>()
  then(onFulfilled = {
    deferred.complete(it)
  }, onRejected = {
    deferred.completeExceptionally(it)
  })
  return deferred
}

suspend fun <T> Promise<T>.await() = toDeferred().await()
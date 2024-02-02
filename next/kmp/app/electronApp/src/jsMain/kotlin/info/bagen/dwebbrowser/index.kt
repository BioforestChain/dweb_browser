package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowController
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_common.view_model.SyncType
import kotlin.js.Promise

@Serializable
class Person(
  @JsName("name")
  val name: String,
  @JsName("id")
  val id: Int
)

fun main() {
  val state = mutableMapOf<dynamic, dynamic>(
    "currentCount" to 10,
    "persons" to mutableListOf(Person("bill", 1), Person("jack", 2))
  )
  val demoComposeApp = ElectronBrowserWindowModule(
    subDomain = "demo.compose.app",
    encodeValueToString = {key: String, value: dynamic, syncType: SyncType ->
      val str = when(key.toString()){
        "currentCount" -> "$value"
        else -> Json.encodeToString<ArrayList<Person>>(value)
      }

      str
    },
    decodeValueFromString = { key: String, value: String, syncType: SyncType ->
      when(key){
        "currentCount" -> value.toInt()
        else -> Json.decodeFromString<Person>(value)
      }
    },
    initVieModelMutableMap = state
  ).apply {
    viewModel.onUpdateByClient{key: String, value: dynamic, syncType ->
      console.error("server received data from client key: value : syncType ", key, ":", value, ":", syncType)
//            viewModel[key] = value + 1
    }
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
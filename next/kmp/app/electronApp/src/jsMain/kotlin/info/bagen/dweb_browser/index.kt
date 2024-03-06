package info.bagen.dweb_browser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowController
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowModule
import org.dweb_browser.js_common.state_compose.ComposeFlow
import org.dweb_browser.js_common.state_compose.state.EmitType
import kotlin.js.Promise

@Serializable
class Person(
  @JsName("name")
  val name: String,
  @JsName("id")
  val id: Int
)

suspend fun main() {

  val count = ComposeFlow.createStateComposeFlowInstance<Int, Int,String>("count_id")
  val persons = ComposeFlow.createListComposeFlowInstance<Person, List<Person>, String>("persons_id")


  // 模拟数据生成在UI实例化之前的情况
  CoroutineScope(Dispatchers.Default).launch {
    count.emitByServer(10, emitType = EmitType.REPLACE)
    persons.emitByServer(listOf(Person("bill", 1)), emitType = EmitType.REPLACE)
  }

  ElectronBrowserWindowModule(
    subDomain = "demo.compose.app",
  ).apply {
    viewModel.composeFlowListAdd(count)
    viewModel.composeFlowListAdd(persons)

    // 测试初始化数据
    // 模拟数据更新
    CoroutineScope(Dispatchers.Default).launch {
      delay(2000)
      count.emitByServer(100, EmitType.REPLACE)
      persons.emitByServer(listOf(Person(name = "bill-2", id = 2)), EmitType.ADD)
      console.log("emitByServer")
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
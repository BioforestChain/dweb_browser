package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowModule
import kotlin.js.Promise

class Person(
  @JsName("name")
  val name: String,
  @JsName("id")
  val id: Int
)

fun main() {
  val state = mutableMapOf<dynamic, dynamic>(
    "currentCount" to 10,
    "persons" to listOf(Person("bill", 1), Person("jack", 2))
  )
  ElectronBrowserWindowModule("demo.compose.app", state)
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
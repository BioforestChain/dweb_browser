package info.bagen.dwebbrowser

import electron.app
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.dweb_browser.js_backend.http.SubDomainHttpServer
import kotlin.js.Promise

suspend fun main() {
//  BrowserDemoReactAppViewModel()
  BrowserDemoComposeAppViewModel()
  SubDomainHttpServer("demo.compose.app").whenReady.await().start()
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
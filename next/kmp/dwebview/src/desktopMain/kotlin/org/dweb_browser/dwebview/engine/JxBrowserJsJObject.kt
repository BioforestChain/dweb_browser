package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import com.teamdev.jxbrowser.js.JsPromise
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.dweb_browser.dwebview.messagePort.DWebMessagePort
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrNull
import org.dweb_browser.helper.getOrPut
import java.util.function.Consumer
import kotlin.reflect.KProperty


@Suppress("MemberVisibilityCanBePrivate")
open class JsJObject(val origin: JsObject) : JsObject by origin {
  private val _jso = RememberLazy(origin) { origin }

  @Suppress("SameParameterValue")
  protected fun <T> staticProp(propName: String) =
    _jso.then { origin.property<T>(propName).get() }

  protected fun <T, R> getProp(p: String, transfer: (T) -> R) =
    transfer(origin.property<T>(p).get())

  protected fun <T : Any?> getProp(p: String) = origin.getProp<T>(p)
  protected fun setProp(p: String, value: Any?) = origin.setProp(p, value)
  protected fun <T : Any?> prop(p: String) = JsJProperty<T>(p)

}

open class JsJEventTarget(js: JsObject) : JsJObject(js) {
  private val jsjWM = WeakHashMap<JsJEventHandler, JsFunctionCallback>()
  fun addEventListener(eventName: String, listener: JsJEventHandler) {
    origin.call<Any>("addEventListener", eventName, jsjWM.getOrPut(listener) {
      JsFunctionCallback {
        listener(JsJEvent(it[0] as JsObject))
      }
    })
  }

  fun removeEventListener(eventName: String, listener: JsJEventHandler) {
    jsjWM[listener]?.also { jsFunctionCallback ->
      origin.call<Any>("removeEventListener", eventName, jsFunctionCallback)
    }
  }

  fun dispatchEvent(event: JsJEvent) {
    dispatchEvent(event.origin)
  }

  fun dispatchEvent(event: JsObject) {
    origin.call<Any>("dispatchEvent", event)
  }
}

class JsJWindow(js: JsObject) : JsJObject(js) {
  private val jsjWM = WeakHashMap<JsJEventHandler, JsFunctionCallback>()
  fun addEventListener(eventName: String, listener: JsJEventHandler) {
    origin.call<Any>("addEventListener", eventName, jsjWM.getOrPut(listener) {
      JsFunctionCallback {
        listener(JsJEvent(it[0] as JsObject))
      }
    })
  }

  fun removeEventListener(eventName: String, listener: JsJEventHandler) {
    jsjWM[listener]?.also { jsFunctionCallback ->
      origin.call<Any>("removeEventListener", eventName, jsFunctionCallback)
    }
  }

  fun postMessage(data: Any, ports: List<DWebMessagePort>) {
    val msgEvent = MessageEvent.new<JsObject>("message", origin.frame().jsObject().apply {
      putProperty("data", data)
      putProperty("ports", ports.map { it.port })
    })
    origin.call<Unit>("dispatchEvent", msgEvent)
    msgEvent.close()
  }

  val scrollX by prop<Double>("scrollX")
  val scrollY by prop<Double>("scrollY")
  val devicePixelRatio by staticProp<Double>("devicePixelRatio")
  val MessageEvent by staticProp<JsObject>("MessageEvent")
}

typealias JsJEventHandler = (JsJEvent) -> Unit

class JsJEvent(js: JsObject) : JsJObject(js) {
  val type by staticProp<String>("type")
  val isTrusted by staticProp<Boolean>("isTrusted")
  val bubbles by staticProp<Boolean>("bubbles")
  val cancelBubble by staticProp<Boolean>("cancelBubble")
  val cancelable by staticProp<Boolean>("cancelable")
  val composed by staticProp<Boolean>("composed")
  val timeStamp by staticProp<Double>("timeStamp")
}

fun Frame.window() = JsJWindow(jsWindow())
fun Frame.jsWindow() = executeJavaScript<JsObject>("window")!!
fun Frame.jsReflect() = executeJavaScript<JsObject>("Reflect")!!
fun Frame.jsObject() = executeJavaScript<JsObject>("({})")!!

fun <T : Any?> JsObject.getProp(p: String) = property<T>(p).getOrNull() as T
fun JsObject.setProp(p: String, value: Any?) = putProperty(p, value)
fun <T : Any?> JsObject.construct(target: JsObject, argArray: List<Any?>) =
  frame().jsReflect().call<T>("construct", target, argArray)

fun <T : JsObject> JsObject.new(vararg argArray: Any?) =
  construct<JsObject>(this, argArray.toList())

class JsJProperty<T : Any?>(val propName: String) {
  operator fun getValue(target: JsJObject, property: KProperty<*>): T {
    return target.origin.getProp(propName)
  }

  operator fun setValue(target: JsJObject, property: KProperty<*>, t: T) {
    target.origin.setProp(propName, t)
  }
}

fun <T> JsPromise.asDeferred(deferred: CompletableDeferred<T> = CompletableDeferred<T>()): Deferred<T> {
  runCatching {
    then {
      runCatching {
        @Suppress("UNCHECKED_CAST")
        deferred.complete(it[0] as T)
      }.getOrElse {
        deferred.completeExceptionally(it)
      }
    }.catchError {
      when (val err = it[0]) {
        is Throwable -> deferred.completeExceptionally(err)
        else -> deferred.completeExceptionally(JsException(err.toString()))
      }
    }
  }.getOrElse { deferred.completeExceptionally(it) }
  return deferred
}

suspend fun <T> JsPromise.await(): T {
  return this.asDeferred<T>().await()
}

suspend fun <T> Frame.executeJavaScriptAsync(code: String) =
  CompletableDeferred<T>().also { deferred ->
    runCatching {
      executeJavaScript(code, Consumer<T> {
        runCatching {
          if (it is T) {
            deferred.complete(it)
          } else {
            deferred.completeExceptionally(JsException("fail to executeJavaScript, got $it"))
          }
        }.getOrElse { deferred.completeExceptionally(it) }
      })
    }.getOrElse { deferred.completeExceptionally(it) }
  }.await()
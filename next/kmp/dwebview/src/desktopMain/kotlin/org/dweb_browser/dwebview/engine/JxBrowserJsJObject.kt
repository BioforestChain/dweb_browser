package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrNull
import org.dweb_browser.helper.getOrPut
import kotlin.reflect.KProperty

@Suppress("MemberVisibilityCanBePrivate")
open class JsJObject(val originJsObject: JsObject) : JsObject by originJsObject {
  private val _jso = RememberLazy(originJsObject) { originJsObject }

  @Suppress("SameParameterValue")
  protected fun <T> staticProp(propName: String) =
    _jso.then { originJsObject.property<T>(propName).get() }

  protected fun <T, R> getProp(p: String, transfer: (T) -> R) =
    transfer(originJsObject.property<T>(p).get())

  protected fun <T : Any?> getProp(p: String) = originJsObject.property<T>(p).getOrNull() as T
  protected fun setProp(p: String, value: Any?) = originJsObject.putProperty(p, value)
  protected fun <T : Any?> prop(p: String) = JsJProperty<T>(p)

  class JsJProperty<T : Any?>(val propName: String) {
    operator fun getValue(target: JsJObject, property: KProperty<*>): T {
      return target.getProp(propName)
    }

    operator fun setValue(target: JsJObject, property: KProperty<*>, t: T) {
      target.setProp(propName, t)
    }
  }

}

open class JsJEventTarget(js: JsObject) : JsJObject(js) {
  private val jsjWM = WeakHashMap<JsJEventHandler, JsFunctionCallback>()
  fun addEventListener(eventName: String, listener: JsJEventHandler) {
    originJsObject.call<Any>("addEventListener", eventName, jsjWM.getOrPut(listener) {
      JsFunctionCallback {
        listener(JsJEvent(it[0] as JsObject))
      }
    })
  }

  fun removeEventListener(eventName: String, listener: JsJEventHandler) {
    jsjWM[listener]?.also { jsFunctionCallback ->
      originJsObject.call<Any>("removeEventListener", eventName, jsFunctionCallback)
    }
  }

  fun dispatchEvent(event: JsJEvent) {
    dispatchEvent(event.originJsObject)
  }

  fun dispatchEvent(event: JsObject) {
    originJsObject.call<Any>("dispatchEvent", event)
  }
}

class JsJWindow(js: JsObject) : JsJObject(js) {
  private val jsjWM = WeakHashMap<JsJEventHandler, JsFunctionCallback>()
  fun addEventListener(eventName: String, listener: JsJEventHandler) {
    originJsObject.call<Any>("addEventListener", eventName, jsjWM.getOrPut(listener) {
      JsFunctionCallback {
        listener(JsJEvent(it[0] as JsObject))
      }
    })
  }

  fun removeEventListener(eventName: String, listener: JsJEventHandler) {
    jsjWM[listener]?.also { jsFunctionCallback ->
      originJsObject.call<Any>("removeEventListener", eventName, jsFunctionCallback)
    }
  }

  val scrollX by prop<Double>("scrollX")
  val scrollY by prop<Double>("scrollY")
  val devicePixelRatio by staticProp<Double>("devicePixelRatio")
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

fun Frame.window() = JsJWindow(executeJavaScript<JsObject>("window")!!)
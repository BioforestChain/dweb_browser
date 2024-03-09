package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.getOrNull
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

class JsWindow(js: JsObject) : JsJObject(js) {
  fun addEventListener(eventName: String, listener: (JsEvent) -> Unit) {
    originJsObject.call<Any>("addEventListener", eventName, JsFunctionCallback {
      listener(JsEvent(it[0] as JsObject))
    })

  }

  val scrollX by prop<Double>("scrollX")
  val scrollY by prop<Double>("scrollY")
  val devicePixelRatio by staticProp<Double>("devicePixelRatio")
}

class JsEvent(js: JsObject) : JsJObject(js) {
  val type by staticProp<String>("type")
  val isTrusted by staticProp<Boolean>("isTrusted")
  val bubbles by staticProp<Boolean>("bubbles")
  val cancelBubble by staticProp<Boolean>("cancelBubble")
  val cancelable by staticProp<Boolean>("cancelable")
  val composed by staticProp<Boolean>("composed")
  val timeStamp by staticProp<Double>("timeStamp")
}

fun Frame.window() = JsWindow(executeJavaScript<JsObject>("window")!!)
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package tsstdlib

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

external interface Set<T> {
    fun entries(): IterableIterator<dynamic /* JsTuple<T, T> */>
    fun keys(): IterableIterator<T>
    fun values(): IterableIterator<T>
    fun add(value: T): Set<T> /* this */
    fun clear()
    fun delete(value: T): Boolean
    fun forEach(callbackfn: (value: T, value2: T, set: Set<T>) -> Unit, thisArg: Any = definedExternally)
    fun has(value: T): Boolean
    var size: Number
}

external interface SetConstructor {
    var prototype: Set<Any>
}
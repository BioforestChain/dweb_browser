package org.dweb_browser.helper.platform

import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.helper.trueAlso
import kotlin.reflect.KClass


interface StateContext {
  fun <I : Any> emitChange(iKClass: KClass<I>, input: I)
//  fun decode<I:Any,O>(iKClass: KClass<I>, output: O): I

}

inline fun <reified T : Any> StateContext.mutableStateFlow(initValue: T) =
  MyMutableStateFlow(this, T::class, MutableStateFlow(initValue))

class MyMutableStateFlow<T : Any>(
  private val ctx: StateContext,
  private val tKClass: KClass<T>,
  private val target: MutableStateFlow<T>
) :
  MutableStateFlow<T> by target {

  override var value: T
    get() = target.value
    set(value) {
      target.value = value
      ctx.emitChange(tKClass, value)
    }

  override suspend fun emit(value: T) {
    target.emit(value)
    ctx.emitChange(tKClass, value)
  }

  override fun tryEmit(value: T): Boolean {
    return target.tryEmit(value).trueAlso {
      ctx.emitChange(tKClass, value)
    }
  }
}

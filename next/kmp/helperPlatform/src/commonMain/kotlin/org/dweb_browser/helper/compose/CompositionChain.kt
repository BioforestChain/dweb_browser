package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import org.dweb_browser.helper.SafeHashMap


/**
 * 将 ProvidedChainValue 进行手动保存并使用。
 * 可以用来替代 currentCompositionLocalContext 和 CompositionLocalProvider 的工具类。
 *
 * TODO: 等待 currentCompositionLocalContext 可用
 *
 * [https://youtrack.jetbrains.com/issue/KT-63869/androidx.compose.runtime.ComposeRuntimeError-Compose-Runtime-internal-error.-Unexpected-or-incorrect-use-of-the-Compose-internal]()
 */
class CompositionChain(val providerMap: Map<CompositionChainKey<out Any?>, ProvidedChainValue<*>> = mapOf()) {
  constructor(providers: Array<out ProvidedChainValue<*>>) : this(providers.associateBy { it.key })

  @Composable
  fun rememberContact(vararg values: ProvidedChainValue<*>): CompositionChain =
    remember(keys = (providerMap.values.toTypedArray() + values)) {
      contact(values = values)
    }

  fun contact(vararg values: ProvidedChainValue<*>) = if (values.isNotEmpty()) {
    CompositionChain(providerMap + values.associateBy { it.key })
  } else {
    this
  }

  @Composable
  operator fun plus(otherChain: CompositionChain): CompositionChain = remember(this, otherChain) {
    contact(otherChain)
  }

  fun contact(otherChain: CompositionChain) = if (otherChain.providerMap.isEmpty()) {
    this
  } else if (providerMap.isEmpty()) {
    otherChain
  } else {
    CompositionChain(
      providerMap + otherChain.providerMap
    )
  }

  @Composable
  fun Provider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
      values = arrayOf(LocalCompositionChain provides this), content
    )
  }

  @Composable
  fun Provider(
    vararg values: ProvidedChainValue<*>,
    content: @Composable () -> Unit,
  ) = rememberContact(values = values).Provider(content)
}

val LocalCompositionChain = compositionLocalOf { CompositionChain() }

fun <T> compositionChainOf(name: String) =
  compositionChainOf<T>(name, compositionLocalOf { noLocalProvidedFor(name) })

fun <T> compositionChainOf(name: String, defaultFactory: () -> T) =
  compositionChainOf(name, compositionLocalOf(defaultFactory = defaultFactory))

internal fun <T> compositionChainOf(name: String, compositionLocal: ProvidableCompositionLocal<T>) =
  CompositionChainKey.from(name, compositionLocal)

class CompositionChainKey<T> private constructor(
  val name: String,
  private val compositionLocal: ProvidableCompositionLocal<T>,
) {
  companion object {
    private val chainKeys = SafeHashMap<ProvidableCompositionLocal<*>, CompositionChainKey<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> from(name: String, compositionLocal: ProvidableCompositionLocal<T>) =
      chainKeys.getOrPut(compositionLocal) {
        CompositionChainKey(name, compositionLocal)
      } as CompositionChainKey<T>
  }

  val current: T
    @Composable get() {
      @Suppress("UNCHECKED_CAST") return (LocalCompositionChain.current.providerMap[this]?.value as T?)/*.also {
        if (it == null) {
          println("LocalCompositionChain.current.providerMap no found $name")
          LocalCompositionChain.current.providerMap.forEach { (key, value) ->
            println("\t key=${key.name} value=${value.value}")
          }
        }
      }*/ ?: compositionLocal.current
    }

  infix fun provides(value: T) = ProvidedChainValue(this, value)
}

class ProvidedChainValue<T> internal constructor(
  val key: CompositionChainKey<T>,
  val value: T,
)
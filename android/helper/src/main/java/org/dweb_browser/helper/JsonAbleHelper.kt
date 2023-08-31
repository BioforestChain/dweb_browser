package org.dweb_browser.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

interface GsonAble<T> {
  fun toJsonAble(): com.google.gson.JsonElement
}

inline fun <reified T> T.toJsonElement() = Json.encodeToJsonElement<T>(this)


val JsonLoose = Json {
  ignoreUnknownKeys = true
}


open class StringEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<String, T>, val getValue: T.() -> String
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeString())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.getValue())
}

open class IntEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<Int, T>, val getValue: T.() -> Int
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeInt())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(value.getValue())
}

open class ByteEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<Byte, T>, val getValue: T.() -> Byte
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeByte())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeByte(value.getValue())
}

open class ProxySerializer<T, P>(
  private val serializer: KSerializer<P>,
  private val valueToProxy: T.() -> P,
  private val proxyToValue: P.() -> T
) : KSerializer<T> {
  override val descriptor: SerialDescriptor = serializer.descriptor

  override fun serialize(encoder: Encoder, value: T): Unit =
    serializer.serialize(encoder, value.valueToProxy())

  override fun deserialize(decoder: Decoder): T = serializer.deserialize(decoder).proxyToValue()
}


open class PropMetasSerializer<T : PropMetas.Constructor<T>>(
  private val propMeta: PropMetas<T>,
) : KSerializer<T> {

  override val descriptor: SerialDescriptor = buildClassSerialDescriptor(propMeta.serialName) {
    for (meta in propMeta.metas) {
      element(meta.propName, meta.descriptor, meta.annotations, meta.nullable)
    }
  }

  override fun deserialize(decoder: Decoder): T {
    val propValues = propMeta.buildValues()
    decoder.decodeStructure(descriptor) {
      if (decodeSequentially()) {
        for ((idx, field) in propMeta.metas.withIndex()) {
          propValues.data[field.propName] = if (field.nullable) decodeNullableSerializableElement(
            descriptor, idx, field.serializer as KSerializer<Any>
          ) else decodeSerializableElement(
            descriptor, idx, field.serializer as KSerializer<Any>
          )
        }
      } else mainLoop@ while (true) {
        when (val idx = decodeElementIndex(descriptor)) {
          CompositeDecoder.DECODE_DONE -> {
            break@mainLoop
          }

          CompositeDecoder.UNKNOWN_NAME -> {
            continue@mainLoop
          }

          else -> {
            val field = propMeta.metas[idx]
            propValues.data[field.propName] = if (field.nullable) decodeNullableSerializableElement(
              descriptor, idx, field.serializer as KSerializer<Any>
            ) else decodeSerializableElement(
              descriptor, idx, field.serializer as KSerializer<Any>
            )
          }
        }
      }
    }
    return propMeta.factory(propValues)
  }

  override fun serialize(encoder: Encoder, value: T) {
    val propValues = value.p
    encoder.encodeStructure(descriptor) {
      for ((idx, field) in propMeta.metas.withIndex()) {
        val propValue = propValues.data[field.propName]
        if (field.nullable) {
          if (propValue == null) continue
          encodeNullableSerializableElement(
            descriptor, idx, field.serializer as KSerializer<Any>, propValue
          )
        } else encodeSerializableElement(
          descriptor, idx, field.serializer as KSerializer<Any>, propValue!!
        )
      }
    }
  }
}


open class PropMetas<T : PropMetas.Constructor<T>>(
  val serialName: String,
  internal val factory: (propValues: PropValues) -> T,
) {
  val metas = mutableListOf<PropMeta<*, *>>()

  val superMetas = mutableSetOf<PropMetas<*>>()
  fun extends(superMeta: PropMetas<*>): PropMetas<T> {
    superMetas.add(superMeta)
    metas += superMeta.metas
    return this
  }

  class PropMeta<T : Any, V : Any?>(
    private val propMap: PropMetas<*>,
    val propName: String,
    val initValue: V,
    val nullable: Boolean,
    val serializer: KSerializer<T>
  ) {
    val descriptor = serializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    val annotations = descriptor.annotations

    init {
      propMap.metas += this
    }

    operator fun invoke(
      propValues: PropValues, initConfig: (PropValueConfig<V>.() -> Unit)? = null
    ) = PropValue(propName, propValues, initConfig)

  }

  class PropValues(internal val data: MutableMap<String, Any?>) {
    fun clone() = PropValues(data.toMutableMap())
    fun set(propName: String, value: Any?) = if (data.containsKey(propName)) {
      data[propName] = value
      true
    } else false

    fun get(propName: String) = data.getOrDefault(propName, null)
  }

  class PropValueConfig<T : Any?>(private val propValue: PropValue<T>) {
    var value: T
      get() = propValue.get()
      set(value) {
        propValue.set(value, false)
      }
    var beforeWrite by Delegates.observable(propValue.beforeWrite) { _, _, newVal ->
      propValue.beforeWrite = newVal
    }
    var afterWrite by Delegates.observable(propValue.afterWrite) { _, _, newVal ->
      propValue.afterWrite = newVal
    }
  }

  class PropValue<T : Any?>(
    val propName: String, val propValues: PropValues, initConfig: (PropValueConfig<T>.() -> Unit)?
  ) {
    internal var beforeWrite: ((newValue: T, oldValue: T) -> T)? = null
    internal var afterWrite: ((newValue: T) -> Unit)? = null

    init {
      if (initConfig != null) {
        PropValueConfig(this).initConfig()
      }
      if (beforeWrite != null) {
        set(get(), true) // 进行一次初始化的写入
      }
    }

    fun set(newValue: T, force: Boolean) {
      val oldValue = get()
      val inputValue = beforeWrite?.invoke(newValue, oldValue) ?: newValue
      if (force || oldValue != inputValue) {
        propValues.data[propName] = inputValue as Any
        afterWrite?.invoke(inputValue)
      }
    }

    fun get() = propValues.data[propName] as T
    operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: T) = set(newValue, false)

    operator fun getValue(thisRef: Any, property: KProperty<*>) = get()
  }

  abstract class Constructor<T : Constructor<T>>(val p: PropValues, private val P: PropMetas<T>) {
    fun assign(other: Constructor<*>) {
      for ((key, value) in other.p.data) {
        if (p.data.containsKey(key)) {
          p.data[key] = value
        }
      }
    }

    fun clone(): T {
      return P.factory(p.clone())
    }
  }

  fun <T : Any> getRequired(propName: String) =
    metas.first { it.propName == propName } as PropMeta<T, T>

  fun <T : Any> getOptional(propName: String) =
    metas.first { it.propName == propName } as PropMeta<T, T?>

  @OptIn(InternalSerializationApi::class)
  inline fun <reified T : Any> required(
    propName: String, initValue: T, serializer: KSerializer<T> = T::class.serializer()
  ) = PropMeta(this, propName, initValue, false, serializer)

  @OptIn(InternalSerializationApi::class)
  inline fun <reified T : Any> optional(
    propName: String, initValue: T? = null, serializer: KSerializer<T> = T::class.serializer()
  ) = PropMeta(this, propName, initValue, true, serializer)


  @OptIn(InternalSerializationApi::class)
  inline fun <reified T : Any> list(
    propName: String,
    initValue: MutableList<T> = mutableListOf(),
    serializer: KSerializer<T> = T::class.serializer()
  ) = PropMeta(
    this, propName, initValue, false, ListSerializer(serializer)
  )

  @OptIn(InternalSerializationApi::class)
  inline fun <reified T : Any> mutableListOptional(
    propName: String,
    initValue: MutableList<T>? = null,
    serializer: KSerializer<T> = T::class.serializer()
  ) = PropMeta(
    this, propName, initValue, true, ListSerializer(serializer)
  )

  fun buildValues() = PropValues(mutableMapOf<String, Any?>().also {
    for (propMeta in metas) {
      it[propMeta.propName] = propMeta.initValue
    }
  })
}


@file:Suppress("UNCHECKED_CAST")

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlin.properties.Delegates
import kotlin.reflect.KProperty


public inline fun <reified T> T.toJsonElement(): JsonElement = Json.encodeToJsonElement<T>(this)
public inline fun <reified T> T.toJsonElement(serializer: KSerializer<T>): JsonElement =
  Json.encodeToJsonElement(serializer, this)

public inline fun <reified T> String.decodeTo(): T = Json.decodeFromString<T>(this)


public val JsonLoose: Json = Json {
  ignoreUnknownKeys = true
}


public open class StringEnumSerializer<T>(
  serialName: String, private val allValues: Map<String, T>, private val getValue: T.() -> String,
) : KSerializer<T> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): T = allValues.getValue(decoder.decodeString())
  override fun serialize(encoder: Encoder, value: T): Unit = encoder.encodeString(value.getValue())
}

public open class IntEnumSerializer<T>(
  serialName: String, private val allValues: Map<Int, T>, private val getValue: T.() -> Int,
) : KSerializer<T> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): T = allValues.getValue(decoder.decodeInt())
  override fun serialize(encoder: Encoder, value: T): Unit = encoder.encodeInt(value.getValue())
}

public open class ByteEnumSerializer<T>(
  serialName: String, private val allValues: Map<Byte, T>, private val getValue: T.() -> Byte,
) : KSerializer<T> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): T = allValues.getValue(decoder.decodeByte())
  override fun serialize(encoder: Encoder, value: T): Unit = encoder.encodeByte(value.getValue())
}

public open class ProxySerializer<T, P>(
  serialName: String,
  private val serializer: KSerializer<P>,
  private val valueToProxy: T.() -> P,
  private val proxyToValue: P.() -> T,
) : KSerializer<T> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

  override fun serialize(encoder: Encoder, value: T): Unit =
    serializer.serialize(encoder, value.valueToProxy())

  override fun deserialize(decoder: Decoder): T = serializer.deserialize(decoder).proxyToValue()
}


public open class PropMetasSerializer<T : PropMetas.Constructor<T>>(
  private val propMeta: PropMetas<T>,
) : KSerializer<T> {

  override val descriptor: SerialDescriptor = buildClassSerialDescriptor(propMeta.serialName) {
    for (meta in propMeta.metas) {
      element(meta.propName, meta.descriptor, meta.annotations, meta.nullable)
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
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

  @OptIn(ExperimentalSerializationApi::class)
  override fun serialize(encoder: Encoder, value: T) {
    val propValues = value.pv
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


public open class PropMetas<T : PropMetas.Constructor<T>>(
  public val serialName: String,
  internal val factory: (propValues: PropValues) -> T,
) {
  public val metas: MutableList<PropMeta<*, *>> = mutableListOf()

  private val superMetas: MutableSet<PropMetas<*>> = mutableSetOf()
  public fun extends(superMeta: PropMetas<*>): PropMetas<T> {
    superMetas.add(superMeta)
    metas += superMeta.metas
    return this
  }

  public class PropMeta<T : Any, V : Any?>(
    propMap: PropMetas<*>,
    public val propName: String,
    public val initValue: V,
    public val nullable: Boolean,
    public val serializer: KSerializer<T>,
  ) {
    public val descriptor: SerialDescriptor = serializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    public val annotations: List<Annotation> = descriptor.annotations

    init {
      propMap.metas += this
    }

    public operator fun invoke(
      propValues: PropValues, initConfig: (PropValueConfig<V>.() -> Unit)? = null,
    ): PropValue<V> = PropValue(propName, propValues, initConfig)

  }

  public class PropValues(internal val data: MutableMap<String, Any?>) {
    public fun clone(): PropValues = PropValues(data.toMutableMap())
    public fun set(propName: String, value: Any?): Boolean = if (data.containsKey(propName)) {
      data[propName] = value
      true
    } else false

    public val keys: Set<String>
      get() = data.keys
    public fun toMap(): Map<String, Any?> {
      return data.toMap()
    }

    //    public fun get(propName: String): Any? = data[propName]
    public fun remove(propName: String): Any? = data.remove(propName)
  }

  public class PropValueConfig<T : Any?>(private val propValue: PropValue<T>) {
    public var value: T
      get() = propValue.get()
      set(value) {
        propValue.set(value, false)
      }
    public var beforeWrite: ((newValue: T, oldValue: T) -> T)? by Delegates.observable(propValue.beforeWrite) { _, _, newVal ->
      propValue.beforeWrite = newVal
    }
    public var afterWrite: ((newValue: T) -> Unit)? by Delegates.observable(propValue.afterWrite) { _, _, newVal ->
      propValue.afterWrite = newVal
    }
  }

  public class PropValue<T : Any?>(
    private val propName: String,
    private val propValues: PropValues,
    initConfig: (PropValueConfig<T>.() -> Unit)?,
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

    public fun set(newValue: T, force: Boolean) {
      val oldValue = get()
      val inputValue = beforeWrite?.invoke(newValue, oldValue) ?: newValue
      if (force || oldValue != inputValue) {
        propValues.data[propName] = inputValue as Any
        afterWrite?.invoke(inputValue)
      }
    }

    public fun get(): T = propValues.data[propName] as T
    public operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: T): Unit =
      set(newValue, false)

    public operator fun getValue(thisRef: Any, property: KProperty<*>): T = get()
  }

  public abstract class Constructor<T : Constructor<T>>(
    public val pv: PropValues,
    private val pm: PropMetas<T>,
  ) {
    public fun assign(other: Constructor<*>) {
      for ((key, value) in other.pv.data) {
        if (pv.data.containsKey(key)) {
          pv.data[key] = value
        }
      }
    }

    public fun clone(): T {
      return pm.factory(pv.clone())
    }
  }

  public fun <T : Any> getRequired(propName: String): PropMeta<T, T> =
    metas.first { it.propName == propName } as PropMeta<T, T>

  public fun <T : Any> getOptional(propName: String): PropMeta<T, T?> =
    metas.first { it.propName == propName } as PropMeta<T, T?>

  @OptIn(InternalSerializationApi::class)
  public inline fun <reified T : Any> required(
    propName: String, initValue: T, serializer: KSerializer<T> = T::class.serializer(),
  ): PropMeta<T, T> = PropMeta(this, propName, initValue, false, serializer)

  @OptIn(InternalSerializationApi::class)
  public inline fun <reified T : Any> optional(
    propName: String, initValue: T? = null, serializer: KSerializer<T> = T::class.serializer(),
  ): PropMeta<T, T?> = PropMeta(this, propName, initValue, true, serializer)


  @OptIn(InternalSerializationApi::class)
  public inline fun <reified T : Any> list(
    propName: String,
    initValue: List<T> = listOf(),
    serializer: KSerializer<T> = T::class.serializer(),
  ): PropMeta<List<T>, List<T>> = PropMeta(
    this, propName, initValue, false, ListSerializer(serializer)
  )

  @OptIn(InternalSerializationApi::class)
  public inline fun <reified T : Any> mutableListOptional(
    propName: String,
    initValue: List<T>? = null,
    serializer: KSerializer<T> = T::class.serializer(),
  ): PropMeta<List<T>, List<T>?> = PropMeta(
    this, propName, initValue, true, ListSerializer(serializer)
  )

  public fun buildValues(): PropValues = PropValues(mutableMapOf<String, Any?>().also {
    for (propMeta in metas) {
      it[propMeta.propName] = propMeta.initValue
    }
  })
}

package info.bagen.kmmsharedmodule.ipc

import kotlinx.serialization.KSerializer
import org.http4k.core.Headers
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class IpcHeaders(private val headersMap: MutableMap<String, String> = mutableMapOf()) :
    KSerializer<IpcHeaders> {
    companion object {
        fun with(headers: Map<String, String>) =
            IpcHeaders().also { it.headersMap += headers }
    }

    constructor(headers: Headers) : this() {
        for (header in headers) {
            header.second?.let {
                headersMap[header.first.asKey()] = it
            }
        }
    }

    fun set(key: String, value: String) {
        headersMap[key.asKey()] = value
    }

    fun init(key: String, value: String) {
        val headerKey = key.asKey()
        if (!headersMap.contains(headerKey)) {
            headersMap[headerKey] = value
        }
    }

    fun get(key: String): String? {
        return headersMap[key.asKey()]
    }

    fun getOrDefault(key: String, default: String) = headersMap[key.asKey()] ?: default

    fun has(key: String): Boolean {
        return headersMap.contains(key.asKey())
    }

    fun delete(key: String) {
        headersMap.remove(key.asKey())
    }

    fun forEach(fn: (key: String, value: String) -> Unit) {
        headersMap.forEach(fn)
    }

    fun toList(): List<Pair<String, String>> {
        return headersMap.toList()
    }

    fun toMap(): MutableMap<String, String> {
        return headersMap
    }

//    override fun serialize(
//        src: IpcHeaders,
//        typeOfSrc: KType?,
//        context: JsonSerializationContext
//    ) = context.serialize(headersMap)
//
//    override fun deserialize(
//        json: JsonElement,
//        typeOfT: KType?,
//        context: JsonDeserializationContext
//    ) = IpcHeaders(context.deserialize<MutableMap<String, String>>(json, MutableMap::class.java))

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())
    override fun serialize(encoder: Encoder, value: IpcHeaders) {
        mapSerializer.serialize(encoder, value.headersMap)
    }

    override fun deserialize(decoder: Decoder): IpcHeaders = IpcHeaders(mapSerializer.deserialize(decoder))

    override val descriptor: SerialDescriptor
        get() = mapSerializer.descriptor

}

private fun <K, V> MutableMap<K, V>.forEach(action: (K, V) -> Unit) {
    for ((key, value) in this.entries) {
        action(key, value)
    }
}

private fun String.asKey(): String {
    return this.lowercase()
}

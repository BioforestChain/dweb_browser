package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import org.http4k.core.Headers
import java.lang.reflect.Type

class IpcHeaders(private val headersMap: MutableMap<String, String> = mutableMapOf()) :
    JsonSerializer<IpcHeaders>, JsonDeserializer<IpcHeaders> {
    companion object {
        fun from(headers: Map<String, String>) =
            IpcHeaders().also { it.headersMap += headers }


        fun from(headers: Headers) = IpcHeaders().also { ipcHeaders ->
            for (header in headers) {
                header.second?.let {
                    ipcHeaders.headersMap[header.first.asKey()] = it
                }
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

    override fun serialize(
        src: IpcHeaders,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ) = context.serialize(headersMap)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ) = IpcHeaders(context.deserialize<MutableMap<String, String>>(json, MutableMap::class.java))
}

private fun String.asKey(): String {
    return this.lowercase()
}

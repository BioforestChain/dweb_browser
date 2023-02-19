package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import org.http4k.core.Method
import java.lang.reflect.Type

/**
 * Ipc 使用的 Method
 */
enum class IpcMethod(val method: String, val http4kMethod: Method) : JsonSerializer<IpcMethod>,
    JsonDeserializer<IpcMethod> {
    GET("GET", Method.GET),
    POST("POST", Method.POST),
    PUT("PUT", Method.PUT),
    DELETE("DELETE", Method.DELETE),
    OPTIONS("OPTIONS", Method.OPTIONS),
    TRACE("TRACE", Method.TRACE),
    PATCH("PATCH", Method.PATCH),
    PURGE("PURGE", Method.PURGE),
    HEAD("HEAD", Method.HEAD),
    ;

    companion object {
        fun from(http4kMethod: Method) = values().first { it.http4kMethod == http4kMethod }
    }

    override fun serialize(
        src: IpcMethod,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ) = JsonPrimitive(src.method)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = json.toString().let { method -> values().first { it.method == method } }

}
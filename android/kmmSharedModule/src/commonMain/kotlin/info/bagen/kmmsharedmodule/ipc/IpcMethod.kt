package info.bagen.kmmsharedmodule.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import kotlinx.serialization.KSerializer
import org.http4k.core.Method
import kotlin.reflect.KType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Encoder

/**
 * Ipc 使用的 Method
 */
@Serializable
enum class IpcMethod(val method: String, val http4kMethod: Method) : KSerializer<IpcMethod> {
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

//    override fun serialize(
//        src: IpcMethod,
//        typeOfSrc: KType?,
//        context: JsonSerializationContext?
//    ) = JsonPrimitive(src.method)
//
//    override fun deserialize(
//        json: JsonElement,
//        typeOfT: KType?,
//        context: JsonDeserializationContext?
//    ) = json.asString.let { method -> values().first { it.method == method } }

    override fun serialize(encoder: Encoder, value: IpcMethod) {

    }
}
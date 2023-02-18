package info.bagen.rust.plaoc.microService.helper

import com.google.gson.*
import java.lang.reflect.Type
import org.http4k.core.Method as Http4kMethod


typealias Mmid = String;

enum class Method(val method: String, val http4kMethod: Http4kMethod) : JsonSerializer<Method>,
    JsonDeserializer<Method> {
    GET("GET", Http4kMethod.GET),
    POST("POST", Http4kMethod.POST),
    PUT("PUT", Http4kMethod.PUT),
    DELETE("DELETE", Http4kMethod.DELETE),
    OPTIONS("OPTIONS", Http4kMethod.OPTIONS),
    TRACE("TRACE", Http4kMethod.TRACE),
    PATCH("PATCH", Http4kMethod.PATCH),
    PURGE("PURGE", Http4kMethod.PURGE),
    HEAD("HEAD", Http4kMethod.HEAD),
    ;

    companion object {
        fun from(http4kMethod: Http4kMethod) = values().first { it.http4kMethod == http4kMethod }
    }

    override fun serialize(
        src: Method,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ) = JsonPrimitive(src.method)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = json.toString().let { method -> values().first { it.method == method } }

}
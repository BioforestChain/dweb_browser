package info.bagen.rust.plaoc.microService.helper

import com.google.gson.JsonSyntaxException
import org.http4k.core.Response
import java.io.InputStream
import java.lang.reflect.Type


fun Response.ok(): Response = if (status.code >= 400) throw Exception("") else this

@Throws(JsonSyntaxException::class)
fun <T> Response.json(typeOfT: Type): T = gson.fromJson(ok().body.stream.reader(), typeOfT)

fun Response.text(): String = ok().bodyString()

fun Response.stream(): InputStream = ok().body.stream

fun Response.int() = ok().json<Int>(Int.javaClass);

fun Response.long() = ok().json<Long>(Long.javaClass);

fun Response.double() = ok().json<Double>(Double.javaClass);

fun Response.float() = ok().json<Float>(Float.javaClass);

fun Response.boolean() = text() == "true"

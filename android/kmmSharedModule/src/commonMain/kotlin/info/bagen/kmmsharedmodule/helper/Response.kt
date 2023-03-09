package info.bagen.kmmsharedmodule.helper

import com.google.gson.JsonSyntaxException
import info.bagen.kmmsharedmodule.ipc.IpcEvent
import kotlinx.serialization.decodeFromString
import org.http4k.core.Response
import java.io.InputStream
import kotlin.reflect.KType
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass


fun Response.ok(): Response = if (status.code >= 400) throw Exception(status.description) else this

@Throws(Exception::class)
fun <T> Response.json(typeOfT: KType): T = Json.decodeFromString<T>(ok().body.stream.reader())

fun Response.text(): String = ok().bodyString()

fun Response.stream(): InputStream = ok().body.stream

fun Response.int() = text().toInt()

fun Response.long() = text().toLong()

fun Response.double() = text().toDouble();

fun Response.float() = text().toFloat();

fun Response.boolean() = text() == "true"

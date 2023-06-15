package org.dweb_browser.microservice.help

import com.google.gson.JsonSyntaxException
import org.http4k.core.Response
import java.io.InputStream
import java.lang.reflect.Type

fun Response.ok(): Response = if (status.code >= 400) throw Exception(status.description) else this

@Throws(JsonSyntaxException::class)
fun <T> Response.json(typeOfT: Type): T = gson.fromJson(ok().body.stream.reader(), typeOfT)

fun Response.text(): String = ok().bodyString()

fun Response.stream(): InputStream = ok().body.stream

fun Response.int() = text().toInt()

fun Response.long() = text().toLong()

fun Response.double() = text().toDouble();

fun Response.float() = text().toFloat();

fun Response.boolean() = text() == "true"

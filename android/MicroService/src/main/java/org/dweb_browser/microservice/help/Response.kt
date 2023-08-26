package org.dweb_browser.microservice.help

import io.ktor.util.moveToByteArray
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.toUtf8
import org.http4k.core.Response
import java.io.InputStream

fun Response.ok(): Response = if (status.code >= 400) throw Exception(status.description) else this

inline fun <reified T> Response.json(): T =
  Json.decodeFromString<T>(ok().body.payload.moveToByteArray().toUtf8())

fun Response.text(): String = ok().bodyString()

fun Response.stream(): InputStream = ok().body.stream

fun Response.int() = text().toInt()

fun Response.long() = text().toLong()

fun Response.double() = text().toDouble();

fun Response.float() = text().toFloat();

fun Response.boolean() = text() == "true"

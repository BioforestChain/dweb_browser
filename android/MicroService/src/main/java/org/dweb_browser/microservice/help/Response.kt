package org.dweb_browser.microservice.help

import io.ktor.util.moveToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.toUtf8
import org.http4k.core.Response
import java.io.InputStream

fun Response.ok(): Response = if (status.code >= 400) throw Exception(status.description) else this

inline fun <reified T> Response.getJsonBody(): T =
  JsonLoose.decodeFromString<T>(ok().body.payload.moveToByteArray().toUtf8())

fun Response.headerJson() = header("Content-Type", "application/json")
fun Response.getJsonBody(value: JsonElement) = body(Json.encodeToString(value)).headerJson()
fun Response.getJsonBody(value: String) = body(value).headerJson()
fun Response.getJsonBody(value: Boolean) = body("$value").headerJson()
fun Response.getJsonBody(value: Number) = body("$value").headerJson()

fun Response.text(): String = ok().bodyString()

fun Response.stream(): InputStream = ok().body.stream

fun Response.int() = text().toInt()

fun Response.long() = text().toLong()

fun Response.double() = text().toDouble();

fun Response.float() = text().toFloat();

fun Response.boolean() = text() == "true"

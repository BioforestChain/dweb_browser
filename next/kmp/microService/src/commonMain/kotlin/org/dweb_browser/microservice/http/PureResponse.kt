package org.dweb_browser.microservice.http

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.Closeable
import kotlinx.serialization.encodeToString
import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.JsonLoose

class PureResponse(
  var statusCode: HttpStatusCode = HttpStatusCode.OK,
  var headers: IpcHeaders = IpcHeaders(),
  var body: IPureBody = IPureBody.Empty,
  var statusText: String? = null,
  var url: String? = null
) : Closeable {

  fun ok() : PureResponse = if (statusCode.value >= 400) throw Exception(statusText ?: statusCode.description) else this

  suspend fun text() = body.toUtf8String()

  suspend fun booleanStrict() = text().toBooleanStrict()

  suspend fun boolean() = text().toBoolean()

  suspend fun int() = text().toInt()

  suspend fun long() = text().toLong()

  suspend fun float() = text().toFloat()

  suspend fun floatOrNull(): Float? = text().toFloatOrNull()

  suspend fun double() = text().toDouble()

  suspend fun doubleOrNull() = text().toDoubleOrNull()

  suspend inline fun <reified T> json() = decodeFromString<T>(text())

  fun jsonBody(value: JsonElement): PureResponse {
    return PureResponse(
      this.statusCode,
      IpcHeaders().apply { set("Content-Type", "application/json") },
      PureUtf8StringBody(
        JsonLoose.encodeToString(value)
      )
    )
  }

  fun jsonBody(value: String): PureResponse {
    return PureResponse(
      this.statusCode,
      IpcHeaders().apply { set("Content-Type", "application/json") },
      PureUtf8StringBody(value)
    )
  }

  fun jsonBody(value: Boolean) = jsonBody("$value")
  fun jsonBody(value: Number) = jsonBody("$value")

  suspend inline fun <reified T> bodyJson() = JsonLoose.decodeFromString<T>(ok().body.toUtf8String())

  override fun close() {
    (body as? PureStreamBody)?.stream?.cancel()
  }
}

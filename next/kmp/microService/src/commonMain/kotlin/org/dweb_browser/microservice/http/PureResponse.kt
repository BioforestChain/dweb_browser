package org.dweb_browser.microservice.http

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.platform.offscreenwebcanvas.FetchResponse
import org.dweb_browser.microservice.ipc.helper.IpcHeaders

data class PureResponse(
  val status: HttpStatusCode = HttpStatusCode.OK,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty,
  val url: String? = null
) {

  fun isOk() = status.value == HttpStatusCode.OK.value
  internal fun ok(): PureResponse =
    if (status.value >= 400) throw Exception(status.description) else this

  suspend fun stream() = ok().body.toPureStream()
  suspend fun text() = ok().body.toPureString()
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
      this.status,
      IpcHeaders().apply { set("Content-Type", "application/json") },
      PureStringBody(
        JsonLoose.encodeToString(value)
      )
    )
  }

  fun jsonBody(value: String) = copy(
    body = PureStringBody(value),
    headers = headers.copy().apply { set("Content-Type", "application/json") })

  fun jsonBody(value: Boolean) = jsonBody("$value")
  fun jsonBody(value: Number) = jsonBody("$value")

  fun body(body: PureStream) = copy(body = PureStreamBody(body))
  fun body(body: ByteReadChannel) = copy(body = PureStreamBody(body))
  fun body(body: ByteArray) = copy(body = PureBinaryBody(body))
  fun body(body: String) = copy(body = PureStringBody(body))
  fun appendHeaders(headers: Iterable<Pair<String, String>>) =
    copy(headers = this.headers.copy().apply {
      for ((key, value) in headers) {
        set(key, value)
      }
    })


}

fun PureResponse.toFetchResponse() =
  FetchResponse(status, headers.toList(), body.toPureStream().getReader("toFetchResponse"))
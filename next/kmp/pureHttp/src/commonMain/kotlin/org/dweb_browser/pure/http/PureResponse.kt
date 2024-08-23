package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.helper.JsonLoose
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

data class PureResponse(
  val status: HttpStatusCode = HttpStatusCode.OK,
  val headers: PureHeaders = PureHeaders(),
  val body: IPureBody = IPureBody.Empty,
  val url: String? = null,
) {

  val isOk get() = status.value in 200..299 || status.value == 101
  internal suspend fun requestOk(): PureResponse =
    if (!isOk) throw PureResponseException(status, body.toPureString())
    else this

  suspend fun stream() = requestOk().body.toPureStream()
  suspend fun text() = requestOk().body.toPureString()
  suspend fun binary() = requestOk().body.toPureBinary()
  suspend fun booleanStrict() = text().toBooleanStrict()
  suspend fun boolean() = text().toBoolean()
  suspend fun int() = text().toInt()
  suspend fun long() = text().toLong()
  suspend fun float() = text().toFloat()
  suspend fun floatOrNull(): Float? = text().toFloatOrNull()
  suspend fun double() = text().toDouble()
  suspend fun doubleOrNull() = text().toDoubleOrNull()

  suspend inline fun <reified T> json() = JsonLoose.decodeFromString<T>(text())
  suspend inline fun <reified T> jsonOrNull() = runCatching { json<T>() }.getOrNull()


  companion object {
    class PureResponseBuilder(cacheDelegate: ReadWriteProperty<Any?, PureResponse>) {
      internal var cache by cacheDelegate
      fun jsonBody(value: String) {
        cache = cache.copy(body = PureStringBody(value),
          headers = cache.headers.copy().apply { set("Content-Type", "application/json") })
      }

      fun jsonBody(value: Boolean) = jsonBody("$value")
      fun jsonBody(value: Number) = jsonBody("$value")
      fun jsonBody(value: JsonElement) = jsonBody(JsonLoose.encodeToString(value))

      fun body(body: PureStream) {
        cache = cache.copy(body = PureStreamBody(body))
      }

      fun body(body: ByteReadChannel) {
        cache = cache.copy(body = PureStreamBody(body))
      }

      fun body(body: ByteArray) {
        cache = cache.copy(body = PureBinaryBody(body))
      }

      fun body(body: String) {
        cache = cache.copy(body = PureStringBody(body))
      }

      fun appendHeaders(headers: Iterable<Pair<String, String>>) {
        cache = cache.copy(headers = cache.headers.copy().apply {
          for ((key, value) in headers) {
            set(key, value)
          }
        })
      }

      fun status(value: HttpStatusCode) {
        cache = cache.copy(status = value)
      }
    }

    inline fun build(
      base: PureResponse = PureResponse(),
      builder: PureResponseBuilder.() -> Unit,
    ): PureResponse {
      var result = base
      PureResponseBuilder(Delegates.observable(result) { _/*property*/, _/*oldValue*/, newValue ->
        result = newValue
      }).builder();
      return result
    }
  }

}


class PureResponseException(status: HttpStatusCode, message: String) :
  Exception("PureResponse not ok: [${status.value}] ${status.description}\n${message}")
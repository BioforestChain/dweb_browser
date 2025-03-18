package org.dweb_browser.core.std.file.ext

import io.ktor.http.ContentType
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.base64UrlString
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.KeyValueStore
import org.dweb_browser.helper.platform.getJsonOrPut
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.sha256Sync
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.ext.FetchHook
import org.dweb_browser.pure.http.ext.mime

private val sharedBlobStore by lazy { KeyValueStore("shared-blob-url-key") }
private val MicroModuleBlobFetchHookCache = WeakHashMap<MicroModule.Runtime, FetchHook>()
private val MicroModuleFetchHookCache = WeakHashMap<MicroModule.Runtime, FetchHook>()
private suspend fun MicroModule.Runtime.createBlobFromUrl(
  url: Url,
  onResponse: (PureResponse) -> Unit = {},
) = nativeFetch(url).let { response ->
  onResponse(response)
  val data = response.binary()
  val mime = response.headers.get("Content-Type")
    ?.let { ct -> ContentType.parse(ct).mime }
  val ext = url.segments.lastOrNull()?.run {
    when (val index = lastIndexOf('.')) {
      -1 -> null
      else -> substring(index + 1, length)
    }
  }
  createBlob(data, mime, ext)
}

@Serializable
class BlobInfo(val url: String, val dateTime: Long = datetimeNow())

const val CACHE_DURATION = 12 * 60 * 60 * 1000

val MicroModule.Runtime.fetchHook
  get() = MicroModuleFetchHookCache.getOrPut(this) {
    { nativeFetch(request.url) }
  }
val MicroModule.Runtime.blobFetchHook
  get() = MicroModuleBlobFetchHookCache.getOrPut(this) {
    {
      var res: PureResponse? = null
      // 如果是 data 协议，那么直接返回构建的 Response 即可
      if (request.url.protocol.name == "data") {
        nativeFetch(request.url)
      } else {
        // 不能直接用url，否则可能会出现key过长的问题
        val key = sha256Sync(request.url.toString().utf8Binary).base64UrlString
        val blobInfo = sharedBlobStore.getJsonOrPut<BlobInfo>(key) {
          BlobInfo(createBlobFromUrl(request.url) { res = it })
        }
        if (datetimeNow() - blobInfo.dateTime > CACHE_DURATION) {
          res = null
        }
        res ?: nativeFetch(blobInfo.url).let { response ->
          when {
            response.isOk -> response
            else -> {
              sharedBlobStore.setString(key, createBlobFromUrl(request.url) { res = it })
              res!!
            }
          }
        }
      }
    }
  }

suspend fun MicroModule.Runtime.createBlob(
  data: ByteArray,
  mime: String? = null,
  ext: String? = null,
) = nativeFetch(
  PureClientRequest(
    buildUrlString("file://file.std.dweb/blob/create") {
      mime?.also { parameters["mime"] = mime }
      ext?.also { parameters["ext"] = ext }
    },
    PureMethod.POST,
    body = PureBinaryBody(data)
  )
).text()
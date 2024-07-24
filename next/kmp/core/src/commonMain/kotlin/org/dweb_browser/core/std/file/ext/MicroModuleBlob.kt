package org.dweb_browser.core.std.file.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.KeyValueStore
import org.dweb_browser.pure.http.ext.FetchHook

private val sharedBlobStore by lazy { KeyValueStore("shared-blob-url-key") }
private val MicroModuleBlobFetchHookCache = WeakHashMap<MicroModule.Runtime, FetchHook>()
val MicroModule.Runtime.blobFetchHook
  get() = MicroModuleBlobFetchHookCache.getOrPut(this) {
    {
      val blobUrl = sharedBlobStore.getStringOrPut(request.url.toString()) {
        val response = nativeFetch(request.url)
        val mime = response.headers.get("Content-Type") ?: ""
        val ext = request.url.pathSegments.lastOrNull()?.substringAfterLast(".", "") ?: ""
        val data = response.body.toPureBinary()
        blobWrite(data, mime, ext)
      }
      nativeFetch(blobUrl)
    }
  }
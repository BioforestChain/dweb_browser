package org.dweb_browser.dwebview

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.toKString
import org.dweb_browser.pure.crypto.hash.ccSha256
import platform.Foundation.NSString
import platform.Foundation.NSUUID
import platform.WebKit.WKWebsiteDataStore


class WKWebViewProfileStore private constructor() : DWebProfileStore {
  companion object {
    @OptIn(ExperimentalForeignApi::class)
    fun nameToIdentifier(name: String) = ccSha256(name.toByteArray()).asUByteArray().usePinned {
      NSUUID(uUIDBytes = it.addressOf(0))
    }

    val instance by lazy { WKWebViewProfileStore() }
  }

  fun getOrCreateProfile(engine: DWebViewEngine, profileName: String = engine.remoteMM.mmid) =
    WKWebViewProfile(
      profileName,
      WKWebsiteDataStore.dataStoreForIdentifier(nameToIdentifier(profileName))
    )

  fun getNoPersistentProfile(engine: DWebViewEngine) = WKWebViewProfile(
    engine.remoteMM.mmid + ".incognito",
    WKWebsiteDataStore.nonPersistentDataStore()
  )

  override suspend fun getAllProfileNames(): List<String> {
    val result = CompletableDeferred<List<String>>()
    WKWebsiteDataStore.fetchAllDataStoreIdentifiers { identifiers ->
      result.complete(identifiers?.map { (it as NSString).toKString() } ?: emptyList())
    }
    return result.await()
  }

  override suspend fun deleteProfile(name: String): Boolean {
    val result = CompletableDeferred<Boolean>()
    WKWebsiteDataStore.removeDataStoreForIdentifier(nameToIdentifier(name)) { err ->
      when (err) {
        null -> result.complete(true)
        else -> result.completeExceptionally(
          Exception(err.description ?: "removeDataStoreForIdentifier $name fail.")
        )
      }
    }
    return result.await()
  }
}

internal val wkWebsiteDataStore get() = WKWebViewProfileStore.instance

actual fun getDwebProfileStoreInstance(): DWebProfileStore = wkWebsiteDataStore

package org.dweb_browser.dwebview

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.platform.keyValueStore
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.crypto.hash.ccSha256
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.WebKit.WKWebsiteDataStore

internal const val DwebProfilesKey = "dweb-profiles"

class WKWebViewProfileStore private constructor() : DWebProfileStore {
  companion object {
    @OptIn(ExperimentalForeignApi::class)
    fun nameToIdentifier(name: String) = ccSha256(name.toByteArray()).asUByteArray().usePinned {
      NSUUID(uUIDBytes = it.addressOf(0))
    }

    val instance by lazy { WKWebViewProfileStore() }
  }

  fun getOrCreateProfile(profileName: String) =
    WKWebViewProfile(
      profileName, WKWebsiteDataStore.dataStoreForIdentifier(nameToIdentifier(profileName))
    ).also {
      allProfiles[profileName] = it
      keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
    }

  private val allIncognitoProfile = SafeHashMap<String, WKWebViewProfile>()
  fun getOrCreateIncognitoProfile(
    profileName: String,
    sessionId: String,
  ): WKWebViewProfile {
    val incognitoProfileName = "$profileName@$sessionId$IncognitoSuffix"
    return allIncognitoProfile.getOrPut(incognitoProfileName) {
      WKWebViewProfile(
        incognitoProfileName, WKWebsiteDataStore.nonPersistentDataStore()
      )
    }
  }

  private val allProfiles by lazy {
    SafeHashMap(
      (keyValueStore.getValues(DwebProfilesKey)
        ?: setOf()).associateWith { null as WKWebViewProfile? }.toMutableMap()
    )
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()

  override suspend fun deleteProfile(name: String): Boolean = when {
    allIncognitoProfile.containsKey(name) -> {
      runCatching {
        removeProfile(allIncognitoProfile[name]!!.store).trueAlso {
          allIncognitoProfile.remove(name)
        }
      }.getOrDefault(false)
    }

    allProfiles.containsKey(name) -> {
      runCatching {
        val store = allProfiles[name]?.store ?: withMainContext {
          WKWebsiteDataStore.dataStoreForIdentifier(nameToIdentifier(name))
        }
        removeProfile(store).trueAlso {
          allProfiles.remove(name)
          keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
        }
      }.getOrDefault(false)
    }

    else -> false
  }

  private suspend fun removeProfile(store: WKWebsiteDataStore): Boolean = withMainContext {
    val allDataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
    val dataRecords = CompletableDeferred<List<*>?>().also { deferred ->
      store.fetchDataRecordsOfTypes(allDataTypes) {
        deferred.complete(it)
      }
    }.await()
    if (dataRecords != null && dataRecords.isNotEmpty()) {
      CompletableDeferred<Unit>().also { deferred ->
        store.removeDataOfTypes(dataTypes = allDataTypes,
          forDataRecords = dataRecords,
          completionHandler = {
            deferred.complete(Unit)
          })
      }.await()
    } else {
      CompletableDeferred<Unit>().also { deferred ->
        store.removeDataOfTypes(dataTypes = allDataTypes,
          modifiedSince = NSDate(timeIntervalSinceReferenceDate = 0.0),
          completionHandler = {
            deferred.complete(Unit)
          })
      }.await()
    }
    when (val storeUuid = store.identifier) {
      null -> true
      else -> CompletableDeferred<Boolean>().also { deferred ->
        WKWebsiteDataStore.removeDataStoreForIdentifier(storeUuid) { err ->
          when (err) {
            null -> deferred.complete(true)
            else -> if (err.code == 1L) {
              // 数据占用，没关系 Data store is in use
              deferred.complete(true)
            } else deferred.completeExceptionally(
              Exception(err.description ?: "removeDataStoreForIdentifier $storeUuid fail.")
            )
          }
        }
      }.await()
    }
  }

}

internal val wkWebsiteDataStore get() = WKWebViewProfileStore.instance

actual fun getDwebProfileStoreInstance(): DWebProfileStore = wkWebsiteDataStore

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

    internal val instance by lazy { WKWebViewProfileStore() }
  }

  /**
   * 注意，该函数必须在主线程中使用
   */
  fun getOrCreateProfile(profileName: ProfileName) = when {
    IDWebView.isEnableProfile ->
      WKWebViewProfile(profileName).also {
        allProfiles[profileName.key] = it
        keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
      }

    else -> NoProfileName().let { profileNameV0 ->
      WKWebViewProfile(profileNameV0)
    }
  }

  private val allIncognitoProfile = SafeHashMap<String, WKWebViewProfile>()

  /**
   * 注意，该函数必须在主线程中使用
   */
  fun getOrCreateIncognitoProfile(
    profileName: ProfileName,
    sessionId: String,
  ): WKWebViewProfile {
    val incognitoProfileName = ProfileIncognitoNameV1.from(profileName, sessionId)
    return allIncognitoProfile.getOrPut(incognitoProfileName.key) {
      WKWebViewProfile(incognitoProfileName)
    }
  }

  private val allProfiles by lazy {
    SafeHashMap(
      (keyValueStore.getValues(DwebProfilesKey)
        ?: setOf()).associateWith { WKWebViewProfile(ProfileName.parse(it)) }.toMutableMap()
    )
  }

  override suspend fun getAllProfileNames() = allProfiles.values.map { it.profileName }.toList()

  override suspend fun deleteProfile(name: ProfileName): Boolean = when {
    allIncognitoProfile.containsKey(name.key) -> {
      runCatching {
        removeProfile(allIncognitoProfile[name.key]!!.store).trueAlso {
          allIncognitoProfile.remove(name.key)
        }
      }.getOrDefault(false)
    }

    allProfiles.containsKey(name.key) -> {
      runCatching {
        val store = allProfiles[name.key]!!.store
        removeProfile(store).trueAlso {
          allProfiles.remove(name.key)
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

actual suspend fun getDwebProfileStoreInstance(): DWebProfileStore = wkWebsiteDataStore

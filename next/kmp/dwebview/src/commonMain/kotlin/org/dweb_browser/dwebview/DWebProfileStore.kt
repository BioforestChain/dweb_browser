package org.dweb_browser.dwebview

expect fun getDwebProfileStoreInstance(): DWebProfileStore

interface DWebProfileStore {
  suspend fun getAllProfileNames(): List<String>
  suspend fun isUsingProfile(name: String): Boolean
  suspend fun deleteProfile(name: String): Boolean
}

package org.dweb_browser.dwebview

expect fun getDwebProfileStoreInstance(): DWebProfileStore

interface DWebProfileStore {
  suspend fun getAllProfileNames(): List<String>
  suspend fun deleteProfile(name: String): Boolean
}

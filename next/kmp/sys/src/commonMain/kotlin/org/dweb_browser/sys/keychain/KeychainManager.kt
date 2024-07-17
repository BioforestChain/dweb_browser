package org.dweb_browser.sys.keychain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.helper.trueAlso

class KeychainManager(
  val keychainRuntime: KeychainNMM.KeyChainRuntime,
  val keychainStore: KeychainStore,
) {
  inner class DetailManager(val manifest: IMicroModuleManifest) {
    val keysState = MutableStateFlow<List<String>?>(null)
    suspend fun refresh() {
      keysState.value = keychainStore.keys(manifest.mmid)
    }

    var password by mutableStateOf<ByteArray?>(null)

    suspend fun getPassword(key: String) =
      runCatching { keychainStore.getItem(manifest.mmid, key) }.getOrNull()

    suspend fun updatePassword(key: String, value: ByteArray) =
      keychainStore.setItem(manifest.mmid, key, value).trueAlso {
        password = value
      }

    suspend fun deletePassword(key: String) =
      keychainStore.deleteItem(manifest.mmid, key).trueAlso {
        refresh()
      }

    init {
      keychainRuntime.scopeLaunch(cancelable = true) {
        refresh()
      }
    }
  }

  var microModuleList by mutableStateOf<List<IMicroModuleManifest>?>(null)
    private set

  suspend fun refreshList() {
    val result = mutableListOf<IMicroModuleManifest>()
    val mmids = keychainStore.mmids()
    for (mmid in mmids) {
      keychainRuntime.bootstrapContext.dns.query(mmid)?.also { result.add(it) }
    }
    microModuleList = result
  }

  init {
    keychainRuntime.scopeLaunch(cancelable = true) {
      refreshList()
    }
  }

  var detailController by mutableStateOf<DetailManager?>(null)
    private set


  /** 打开详情界面*/
  fun openDetail(mm: IMicroModuleManifest) {
    detailController = DetailManager(mm)
  }

  fun closeDetail() {
    detailController = null
  }

}

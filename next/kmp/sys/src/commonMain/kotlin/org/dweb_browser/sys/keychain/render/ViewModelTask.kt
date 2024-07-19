package org.dweb_browser.sys.keychain.render

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.sha256Sync
import org.dweb_browser.sys.keychain.render.KeychainAuthentication.Companion.ROOT_KEY_METHOD
import org.dweb_browser.sys.keychain.render.KeychainAuthentication.Companion.ROOT_KEY_TIP
import org.dweb_browser.sys.keychain.render.KeychainAuthentication.Companion.ROOT_KEY_VERIFY

sealed class ViewModelTask(val method: KeychainMethod) {
  abstract val task: CompletableDeferred<ByteArray>
  abstract suspend fun finish(): Boolean
  fun refuse(reason: Throwable) {
    task.completeExceptionally(reason)
  }
}

abstract class RegisterViewModelTask(method: KeychainMethod) :
  ViewModelTask(method) {
  protected abstract fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray
  var registering by mutableStateOf(false)
  override suspend fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    registering = true
    return runCatching {
      keychainMetadataStore.setItem(ROOT_KEY_METHOD, method.method.utf8Binary)
      val keyRawData = doFinish { keyTip ->
        keychainMetadataStore.setItem(ROOT_KEY_TIP, keyTip)
      }
      keychainMetadataStore.setItem(ROOT_KEY_VERIFY, sha256Sync(keyRawData))
      task.complete(keyRawData)

      true
    }.getOrElse {
      false
    }.also {
      registering = false
    }
  }
}

abstract class VerifyViewModelTask(method: KeychainMethod) :
  ViewModelTask(method) {
  abstract fun keyTipCallback(keyTip: ByteArray?)

  protected abstract fun doFinish(): ByteArray
  open suspend fun verifyUser(keyRawData: ByteArray): Boolean {
    return sha256Sync(keyRawData).contentEquals(
      keychainMetadataStore.getItem(ROOT_KEY_VERIFY)
        ?: throw Exception("no found root-key verify")
    )
  }

  init {
    val regMethod = getRegisteredMethod()
    if (regMethod != method) {
      WARNING("invalid root-key method, expect=${method} actual=$regMethod")
    }
    keychainMetadataStore.getItem(ROOT_KEY_TIP).also {
      keyTipCallback(it)
    }
  }

  var verifying by mutableStateOf(false)
  override suspend fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    val keyRawData = doFinish()
    verifying = true
    return runCatching {
      verifyUser(keyRawData).trueAlso {
        task.complete(keyRawData)
      }
    }.getOrElse { false }.also { verifying = false }
  }
}
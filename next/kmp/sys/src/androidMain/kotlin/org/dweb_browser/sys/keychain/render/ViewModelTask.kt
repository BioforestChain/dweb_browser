package org.dweb_browser.sys.keychain.render

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.jvmSha256
import org.dweb_browser.sys.keychain.deviceKeyStore

sealed class ViewModelTask(val method: KeychainMethod) {
  abstract val task: CompletableDeferred<ByteArray>
  abstract fun finish(): Boolean
}

abstract class RegisterViewModelTask(method: KeychainMethod) : ViewModelTask(method) {
  protected abstract fun doFinish(keyTipCallback: (ByteArray) -> Unit): ByteArray
  override fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    deviceKeyStore.setItem("root-key-method", method.method)
    val keyRawData = doFinish { keyTip ->
      deviceKeyStore.setRawItem("root-key-tip".utf8Binary, keyTip)
    }
    deviceKeyStore.setRawItem("root-key-verify".utf8Binary, jvmSha256(keyRawData))
    task.complete(keyRawData)
    return true
  }
}

abstract class VerifyViewModelTask(method: KeychainMethod) : ViewModelTask(method) {
  abstract fun keyTipCallback(keyTip: ByteArray?)

  private val keyVerify = deviceKeyStore.getRawItem("root-key-verify".utf8Binary)
    ?: throw Exception("no found root-key verify")

  init {
    deviceKeyStore.getItem("root-key-method").also { m ->
      if (m != method.method) {
        throw Exception("invalid root-key method, expect=${method.method} actual=$m")
      }
    }
    deviceKeyStore.getRawItem("root-key-tip".utf8Binary).also {
      keyTipCallback(it)
    }
  }

  protected abstract fun doFinish(): ByteArray
  override fun finish(): Boolean {
    if (task.isCompleted) {
      return true
    }
    val keyRawData = doFinish()
    return jvmSha256(keyRawData).contentEquals(keyVerify).trueAlso {
      task.complete(keyRawData)
    }
  }
}
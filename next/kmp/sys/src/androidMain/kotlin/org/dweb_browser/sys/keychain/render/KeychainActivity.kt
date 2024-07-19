package org.dweb_browser.sys.keychain.render

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.keychain.KeychainNMM

class KeychainActivity : ComponentActivity() {
  companion object {
    private val creates = mutableMapOf<String, CompletableDeferred<KeychainActivity>>()
    suspend fun create(runtime: MicroModule.Runtime): KeychainActivity {
      val uid = randomUUID()
      val deferred = CompletableDeferred<KeychainActivity>()
      creates[uid] = deferred
      deferred.invokeOnCompletion { creates.remove(uid) }

      runtime.startAppActivity(KeychainActivity::class.java) { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("uid", uid)
      }
      return deferred.await()
    }
  }

  private val auth = KeychainAuthentication(
    onAuthRequestDismiss = { finish() },
    lifecycleScope = lifecycleScope,
  )

  suspend fun start(
    runtime: KeychainNMM.KeyChainRuntime,
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
  ) = auth.start(runtime, title, subtitle, description)

  private var uid = ""

  override fun onResume() {
    super.onResume()
    uid = intent.getStringExtra("uid") ?: return finish()
    creates[uid]?.complete(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      auth.Render()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    auth.viewModelTask.completeExceptionally(CancellationException("User cancel"))
  }
}

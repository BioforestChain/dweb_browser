package org.dweb_browser.sys.keychain.render

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.randomUUID

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

  private val auth = KeychainAuthentication(lifecycleScope) { finish() }
  suspend fun start(
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
    background: (@Composable (Modifier) -> Unit)? = null,
  ) = auth.start(title, subtitle, description, background)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.getStringExtra("uid")?.also {
      creates[it]?.complete(this)
    }
    setContent {
      auth.Render()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    auth.viewModelTask.completeExceptionally(CancellationException("User cancel"))
  }
}

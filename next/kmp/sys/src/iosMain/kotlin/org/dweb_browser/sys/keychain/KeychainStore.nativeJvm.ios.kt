package org.dweb_browser.sys.keychain

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.keychain.render.KeychainAuthentication
import platform.UIKit.UIApplication
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.sheetPresentationController
import kotlin.coroutines.cancellation.CancellationException

actual suspend fun tryThrowUserRejectAuth(
  runtime: KeychainNMM.KeyChainRuntime,
  remoteMmid: MMID,
  title: String,
  description: String,
) = CompletableDeferred<Unit>().also { deferred ->
  runCatching {
    withMainContext {
      var reject: (reason: String?) -> Unit = { }
      val contentController = ComposeUIViewController({
        this.delegate = object : ComposeUIViewControllerDelegate {
          override fun viewDidDisappear(animated: Boolean) {
            reject("User cancel")
          }
        }
      }) {
        val scope = rememberCoroutineScope()
        val auth = remember {
          KeychainAuthentication(
            onAuthRequestDismiss = { reject("User cancel") },
            lifecycleScope = scope,
          )
        }
        auth.ContentRender(closeBoolean = false, Modifier.fillMaxSize())
        LaunchedEffect(Unit) {
          val subtitle =
            runtime.bootstrapContext.dns.query(remoteMmid)?.name?.let { "$it($remoteMmid)" }
              ?: remoteMmid
          auth.start(runtime, title, subtitle, description)
          deferred.complete(Unit)

          // 关闭窗口
          reject(null)
        }
      }

      reject = { reason ->
        if (!deferred.isCompleted) {
          @Suppress("ThrowableNotThrown") deferred.completeExceptionally(
            CancellationException(reason)
          )
        }
        contentController.dismissModalViewControllerAnimated(true)
      }

      contentController.sheetPresentationController?.also { sheet ->
        sheet.detents = listOf(UISheetPresentationControllerDetent.mediumDetent())
        sheet.prefersGrabberVisible = true
      }

      UIApplication.sharedApplication.keyWindow?.rootViewController?.apply {
        presentViewController(
          viewControllerToPresent = contentController,
          animated = true,
          completion = null
        )
      } ?: reject("No Application Window")
    }
  }.getOrElse { deferred.completeExceptionally(it) }
}.await()
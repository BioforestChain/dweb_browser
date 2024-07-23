package org.dweb_browser.sys.keychain

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.keychain.render.KeychainAuthentication
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.hasMainWindow
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

actual suspend fun tryThrowUserRejectAuth(
  runtime: KeychainNMM.KeyChainRuntime,
  remoteMmid: MMID,
  title: String,
  description: String,
) = CompletableDeferred<Unit>().also { deferred ->
  runCatching {
    val fromMainWindow = if (runtime.hasMainWindow) runtime.getMainWindow()
    else null
    val fromWindow = fromMainWindow?.pureViewController?.asDesktop()?.getComposeWindowOrNull()
    var reject: (reason: String?) -> Unit = {}
    // 应用模态窗口，会阻塞其它所有窗口
    withMainContext {
      ComposeDialog(fromWindow, Dialog.ModalityType.APPLICATION_MODAL).apply {
        /// 禁用默认的关闭按钮的行为
        addWindowListener(object : WindowAdapter() {
          @Override
          override fun windowClosing(e: WindowEvent?) {
            reject("window close")
          }
        })
        minimumSize = Dimension(300, 300)
        maximumSize = Dimension(800, 600)
        size = Dimension(480, 400)
        // Center the dialog on the screen
        fun centerLocation() {
          val screenSize = (fromWindow?.toolkit ?: Toolkit.getDefaultToolkit()).screenSize
          location =
            Point((screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2)
        }
        centerLocation()
        addComponentListener(object : ComponentAdapter() {
          override fun componentResized(e: ComponentEvent?) {
            super.componentResized(e)
            centerLocation()
          }
        })

        // 设置渲染
        setContent {
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

        // 注册销毁函数
        reject = { reason ->
          if (!deferred.isCompleted) {
            @Suppress("ThrowableNotThrown") deferred.completeExceptionally(
              CancellationException(reason)
            )
          }
          isVisible = false
          dispose()
        }
        // 显示
        isVisible = true
        isAlwaysOnTop = true
      }
    }
  }.getOrElse { deferred.completeExceptionally(it) }
}.await()
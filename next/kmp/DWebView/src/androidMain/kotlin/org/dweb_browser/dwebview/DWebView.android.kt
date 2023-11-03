package org.dweb_browser.dwebview

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.utils.io.CancellationException
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine

class DWebView(private val engine: DWebViewEngine) : IDWebView {
  suspend fun <T> runMain(block: suspend CoroutineScope.() -> T) =
    engine.mainScope.async(block = block).await()

  private suspend fun loadUrl(task: LoadUrlTask) = runMain {
    engine.loadUrl(task.url)
    val off = engine.addWebViewClient(object : WebViewClient() {
      override fun onReceivedError(
        view: WebView, request: WebResourceRequest, error: WebResourceError
      ) {
        task.deferred.completeExceptionally(Exception("[${error.errorCode}] ${error.description}"))
      }

      override fun onPageFinished(view: WebView, url: String) {
        task.deferred.complete(url)
      }
    })
    task.deferred.invokeOnCompletion {
      off()
    }
    task.deferred.await()
  }

  private val loadUrlTask = atomic<LoadUrlTask?>(null)

  override suspend fun loadUrl(url: String, force: Boolean) = loadUrlTask.getAndUpdate { preTask ->
    if (!force && preTask?.url == url) {
      return@getAndUpdate preTask
    } else {
      preTask?.deferred?.cancel(CancellationException("load new url: $url"));
    }
    val newTask = LoadUrlTask(url)
    loadUrl(newTask)
    newTask.deferred.invokeOnCompletion {
      loadUrlTask.getAndUpdate { preTask ->
        if (preTask == newTask) null else preTask
      }
    }
    newTask
  }!!.deferred.await()

  override suspend fun getUrl() = runMain {
    engine.url ?: "about:blank"
  }


  override suspend fun getTitle(): String {
    return engine.title ?: ""
  }

  override suspend fun getIcon(): String {
    val GET_ICON_CODE = """
(function getAndroidIcon(preference_size = 64) {
  const iconLinks = [
    ...document.head.querySelectorAll(`link[rel*="icon"]`).values(),
  ]
    .map((ele) => {
      return {
        ele,
        rel: ele.getAttribute("rel"),
      };
    })
    .filter((link) => {
      return (
        link.rel === "icon" ||
        link.rel === "shortcut icon" ||
        link.rel === "apple-touch-icon" ||
        link.rel === "apple-touch-icon-precomposed"
      );
    })
    .map((link, index) => {
      const sizes = parseInt(link.ele.getAttribute("sizes")) || 32;
      return {
        ...link,
        // 上古时代的图标默认大小是32
        sizes,
        weight: sizes * 100 + index,
      };
    })
    .sort((a, b) => {
      const a_diff = Math.abs(a.sizes - preference_size);
      const b_diff = Math.abs(b.sizes - preference_size);
      /// 和预期大小接近的排前面
      if (a_diff !== b_diff) {
        return a_diff - b_diff;
      }
      /// 权重大的排前面
      return b.weight - a.weight;
    });

  const href =
    (
      iconLinks
        /// 优先不获取 ios 的指定图标
        .filter((link) => {
          return (
            link.rel !== "apple-touch-icon" &&
            link.rel !== "apple-touch-icon-precomposed"
          );
        })[0] ??
      /// 获取标准网页图标
      iconLinks[0]
    )?.ele.href ?? "favicon.ico";

  const iconUrl = new URL(href, document.baseURI);
  return iconUrl.href;
})()
""".trimIndent()
    return runMain {
      engine.evaluateSyncJavascriptCode(GET_ICON_CODE)
    }
  }

  override suspend fun destroy() = runMain {
    engine.destroy()
  }

  override suspend fun canGoBack() = runMain { engine.canGoBack() }

  override suspend fun canGoForward() = runMain { engine.canGoForward() }

  override suspend fun goBack() = runMain {
    return@runMain if (engine.canGoBack()) {
      engine.goBack()
      true// TODO 能否有goBack钩子？
    } else {
      false
    }
  }

  override suspend fun goForward() = runMain {
    return@runMain if (engine.canGoForward()) {
      engine.goForward()
      true// TODO 能否有goForward钩子？
    } else {
      false
    }
  }

  override suspend fun createMessageChannel(): IMessageChannel {
    return DWebMessageChannel(engine.createWebMessageChannel())
  }

  override suspend fun setContentScale(scale: Float) = runMain {
    engine.setInitialScale((scale * 100).toInt())
  }

  override fun evalAsyncJavascript(code: String): Deferred<String> {
    val comp = CompletableDeferred<String>()
    engine.mainScope.launch {
      val result = engine.evaluateAsyncJavascriptCode(code)
      comp.complete(result)
    }
    return comp
  }


  //#region 一些针对平台的接口
  fun getAndroidWebViewEngine() = engine
  //#endregion
}

data class MessageEvent(override val data: String, override val ports: List<IWebMessagePort> = emptyList()) : IMessageEvent


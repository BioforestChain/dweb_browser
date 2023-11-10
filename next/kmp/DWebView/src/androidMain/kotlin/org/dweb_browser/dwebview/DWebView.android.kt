package org.dweb_browser.dwebview

import android.content.Context
import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.utils.io.CancellationException
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.dwebview.DWebMessagePort.Companion.into
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.withMainContext

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView =
  create(NativeMicroModule.Companion.getAppContext(), mm, options)

suspend fun IDWebView.Companion.create(
  /**
   * 一个WebView的上下文
   */
  context: Context,
  /// 这两个参数是用来实现请求拦截与转发的
  remoteMM: MicroModule,
  /**
   * 一些DWebView自定义的参数
   */
  options: DWebViewOptions = DWebViewOptions(),
  /**
   * 该参数的存在，是用来做一些跟交互式界面相关的行为的，交互式界面需要有一个上下文，比如文件选择、权限申请等行为。
   * 我们将这些功能都写到了BaseActivity上，如果没有提供该对象，则相关的功能将会被禁用
   */
  activity: BaseActivity? = null
) = withMainContext { DWebView(DWebViewEngine(context, remoteMM, options, activity)) }

class DWebView(private val engine: DWebViewEngine) : IDWebView {
  private suspend fun loadUrl(task: LoadUrlTask) = withMainContext {
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

  override suspend fun getUrl() = withMainContext {
    engine.url ?: "about:blank"
  }


  override suspend fun getTitle() = withMainContext {
    engine.title ?: ""
  }

  override suspend fun getIcon() = withMainContext {
    engine.evaluateSyncJavascriptCode(
      """
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
"""
    )
  }

  override suspend fun destroy() = withMainContext {
    engine.destroy()
  }

  override suspend fun canGoBack() = withMainContext { engine.canGoBack() }

  override suspend fun canGoForward() = withMainContext { engine.canGoForward() }

  override suspend fun goBack() = withMainContext {
    if (engine.canGoBack()) {
      engine.goBack()
      true// TODO 能否有goBack钩子？
    } else {
      false
    }
  }

  override suspend fun goForward() = withMainContext {
    if (engine.canGoForward()) {
      engine.goForward()
      true// TODO 能否有goForward钩子？
    } else {
      false
    }
  }

  override suspend fun createMessageChannel(): IWebMessageChannel = withMainContext {
    DWebMessageChannel(engine.createWebMessageChannel())
  }

  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) = withMainContext {
    engine.postWebMessage(WebMessage(data, ports.map { it.into() }.toTypedArray()), Uri.EMPTY)
  }

  override suspend fun setContentScale(scale: Float) = withMainContext {
    engine.setInitialScale((scale * 100).toInt())
  }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit
  ): String = engine.evaluateAsyncJavascriptCode(script, afterEval)

  override fun onDestroy(cb: SimpleCallback) = engine.onDestroy(cb)
  override suspend fun onReady(cb: SimpleCallback) = engine.onReady(cb)


  //#region 一些针对平台的接口
  fun getAndroidWebViewEngine() = engine
  //#endregion
}


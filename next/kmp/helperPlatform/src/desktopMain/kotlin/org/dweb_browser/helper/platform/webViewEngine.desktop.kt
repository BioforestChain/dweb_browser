package org.dweb_browser.helper.platform

import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.net.HttpHeader
import com.teamdev.jxbrowser.net.HttpStatus
import com.teamdev.jxbrowser.net.Scheme
import com.teamdev.jxbrowser.net.UrlRequestJob
import com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback.Response
import com.teamdev.jxbrowser.profile.Profile
import com.teamdev.jxbrowser.profile.Profiles
import org.dweb_browser.helper.utf8ToBase64UrlString
import org.dweb_browser.platform.desktop.os.dataDir
import org.dweb_browser.platform.desktop.webview.LowLevelWebEngineAPI
import org.dweb_browser.platform.desktop.webview.jxBrowserEngine
import java.nio.file.Paths
import kotlin.io.path.name

object webViewEngine {
  val webviewDir = dataDir.parent.resolve("dwebview")

  private val dwebviewEngineOptionsBuilder: (EngineOptions.Builder.() -> Unit) = {
    // 拦截dweb deeplink
    addScheme(Scheme.of("dweb")) { params ->
      DeepLinkHook.instance.emitLink(params.urlRequest().url())

      val job = params.newUrlRequestJob(
        UrlRequestJob.Options //
          .newBuilder(HttpStatus.OK) //
          .addHttpHeader(HttpHeader.of("Access-Control-Allow-Origin", "*")) //
          .build() //
      )
      job.write(byteArrayOf())
      job.complete()
      Response.intercept(job)
    }

    addSwitch("--enable-experimental-web-platform-features")
  }

  @OptIn(LowLevelWebEngineAPI::class)
  val hardwareAcceleratedEngine
    get() =
      jxBrowserEngine.hardwareAccelerated(webviewDir, dwebviewEngineOptionsBuilder)


  @OptIn(LowLevelWebEngineAPI::class)
  val offScreenEngine
    get() =
      jxBrowserEngine.offScreen(webviewDir, dwebviewEngineOptionsBuilder)

  fun resolveDir(dir: String): String {
    val info = webviewDir.relativize(Paths.get(dir))
    return info.name
  }
}

fun Profiles.getOrCreateProfile(profileName: String): Profile =
  list().firstOrNull { it.name() == profileName } ?: newProfile(profileName)

fun Profiles.getOrCreateIncognitoProfile(profileName: String, sessionId: String): Profile =
  "$profileName@${sessionId.utf8ToBase64UrlString}".let { fullName ->
    list().firstOrNull { it.name() == fullName }
      ?: newIncognitoProfile(fullName)
  }

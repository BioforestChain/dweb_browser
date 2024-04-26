package org.dweb_browser.platform.desktop.webview

import com.teamdev.jxbrowser.VersionInfo
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.engine.ProprietaryFeature
import com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN

private const val KEY = "jxbrowser.license.key"
private const val LICENSE = ""

object WebviewEngine {
  val licenseKey = (System.getenv(KEY) ?: System.getProperty(KEY) ?: LICENSE)

  fun hardwareAccelerated(optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null): Engine =
    Engine.newInstance(EngineOptions.newBuilder(HARDWARE_ACCELERATED).run {
      optionsBuilder?.invoke(this)
      enableProprietaryFeature(ProprietaryFeature.HEVC)
      enableProprietaryFeature(ProprietaryFeature.WIDEVINE)
      enableProprietaryFeature(ProprietaryFeature.AAC)
      enableProprietaryFeature(ProprietaryFeature.H_264)
      this.licenseKey(licenseKey)
      build()
    })

  fun offScreen(optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null): Engine =
    Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).run {
      optionsBuilder?.invoke(this)
      this.licenseKey(licenseKey)
      build()
    })

  val offScreen by lazy { offScreen() }

  val chromiumVersion by lazy { VersionInfo.chromiumVersion() }


  init {
    println("QWQ chromiumVersion=$chromiumVersion")
  }
}

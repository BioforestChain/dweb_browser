package org.dweb_browser.platform.desktop.webview

import com.teamdev.jxbrowser.VersionInfo
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.engine.ProprietaryFeature
import com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN
import com.teamdev.jxbrowser.engine.event.EngineClosed
import kotlinx.atomicfu.locks.SynchronizedObject
import java.nio.file.Path as NioPath

private const val KEY = "jxbrowser.license.key"
private const val LICENSE = ""

object WebviewEngine {
  val licenseKey = (System.getenv(KEY) ?: System.getProperty(KEY) ?: LICENSE)
  private val engineLock = SynchronizedObject()
  private val engineMaps = mutableMapOf<String, Engine>()

  init {
    /// 监听进程死亡，那么就关闭所有的 engine
    Runtime.getRuntime().addShutdownHook(Thread {
      synchronized(engineLock) {
        engineMaps.values.forEach { it.close() }
        engineMaps.clear()
      }
    })
  }

  private fun getOrCreateByDir(dir: NioPath, createEngine: () -> Engine) =
    synchronized(engineLock) {
      val key = dir.toAbsolutePath().toString()
      engineMaps.getOrPut(key) {
        createEngine().also {
          it.on(EngineClosed::class.java) {
            synchronized(engineLock) {
              engineMaps.remove(key)
            }
          }
        }
      }
    }

  fun hardwareAccelerated(
    dataDir: NioPath,
    optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null,
  ): Engine = getOrCreateByDir(dataDir) {
    Engine.newInstance(EngineOptions.newBuilder(HARDWARE_ACCELERATED).run {
      optionsBuilder?.invoke(this)
      // 设置用户数据目录，这样WebApp退出再重新打开时能够读取之前的数据
      userDataDir(dataDir)
      // 加入音视频解码器的支持
      enableProprietaryFeature(ProprietaryFeature.HEVC)
      enableProprietaryFeature(ProprietaryFeature.WIDEVINE)
      enableProprietaryFeature(ProprietaryFeature.AAC)
      enableProprietaryFeature(ProprietaryFeature.H_264)
      this.licenseKey(licenseKey)
      build()
    })
  }


  fun offScreen(
    dataDir: NioPath,
    optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null,
  ): Engine = getOrCreateByDir(dataDir) {
    Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).run {
      optionsBuilder?.invoke(this)
      // 设置用户数据目录，这样WebApp退出再重新打开时能够读取之前的数据
      userDataDir(dataDir)
      this.licenseKey(licenseKey)
      build()
    })
  }

  val chromiumVersion by lazy { VersionInfo.chromiumVersion() }


  init {
    println("QWQ chromiumVersion=$chromiumVersion")
  }
}

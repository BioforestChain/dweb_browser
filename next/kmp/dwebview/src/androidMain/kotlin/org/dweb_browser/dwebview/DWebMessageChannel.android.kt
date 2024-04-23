package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.WebMessagePortCompat
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.dwebview.engine.DWebViewEngine

@SuppressLint("RestrictedApi")
class DWebMessageChannel internal constructor(
  webMessageChannel: Array<WebMessagePortCompat>,
  engine: DWebViewEngine,
) : IWebMessageChannel {
  override val port1: DWebMessagePort = DWebMessagePort.from(webMessageChannel[0], engine)
  override val port2: DWebMessagePort = DWebMessagePort.from(webMessageChannel[1], engine)
}

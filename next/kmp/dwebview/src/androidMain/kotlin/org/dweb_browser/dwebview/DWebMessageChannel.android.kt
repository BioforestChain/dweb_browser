package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.WebMessagePortCompat

@SuppressLint("RestrictedApi")
class DWebMessageChannel internal constructor(
  webMessageChannel: Array<WebMessagePortCompat>,
) : IWebMessageChannel {
  override val port1: DWebMessagePort = DWebMessagePort.from(webMessageChannel[0])
  override val port2: DWebMessagePort = DWebMessagePort.from(webMessageChannel[1])
}

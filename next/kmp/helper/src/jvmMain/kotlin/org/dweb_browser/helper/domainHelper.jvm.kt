package org.dweb_browser.helper

import java.net.IDN
import java.net.InetAddress

public actual fun String.toPunyCode(): String = IDN.toASCII(this)!!

public actual fun String.isRealDomain(): Boolean = try {
  InetAddress.getByName(this)
  true
} catch (e: Throwable) {
  false
}

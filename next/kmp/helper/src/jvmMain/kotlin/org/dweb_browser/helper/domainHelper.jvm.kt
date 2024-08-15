package org.dweb_browser.helper

import java.net.IDN
import java.net.InetAddress

public actual fun String.toPunyCode() = IDN.toASCII(this)!!

public actual fun String.isRealDomain() = try {
  InetAddress.getByName(this)
  true
} catch (e: Throwable) {
  false
}

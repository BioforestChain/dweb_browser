package org.dweb_browser.helper

import java.net.IDN

actual fun String.toPunyCode() = IDN.toASCII(this)!!

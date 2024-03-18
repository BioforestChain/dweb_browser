package org.dweb_browser.helper

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.AF_INET
import platform.posix.SOCK_DGRAM
import platform.posix.addrinfo
import platform.posix.getaddrinfo

@OptIn(ExperimentalForeignApi::class)
actual fun String.isRealDomain() = memScoped {
  val hints: addrinfo = alloc()
  hints.ai_family = AF_INET
  hints.ai_socktype = SOCK_DGRAM
  val res: CPointerVar<addrinfo> = alloc()

  val err = getaddrinfo(this@isRealDomain, null, hints.ptr, res.ptr)
  err == 0
}

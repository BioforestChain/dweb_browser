package org.dweb_browser.shared

interface Platform {
  val name: String
}

expect fun getPlatform(): Platform
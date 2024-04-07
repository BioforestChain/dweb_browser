package org.dweb_browser.platform.desktop.os

enum class OsType(val type: String) {
  MacOS("Mac OS"),
  M2("MacOS"),
  Windows("Windows"),
  Linux("Linux"),
  Unknown("unknown"),
  ;

  companion object {
    //    val ALL_VALUES = OsType.entries.associateBy { it.type }
    val current by lazy {
      val osName = System.getProperty("os.name")
      if (osName.contains("Mac OS"))
        MacOS
      else if (osName.contains("Windows"))
        Windows
      else if (osName.contains("Linux"))
        Linux
      else Unknown
    }
  }
}
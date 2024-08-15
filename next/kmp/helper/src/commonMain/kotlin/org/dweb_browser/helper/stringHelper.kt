package org.dweb_browser.helper

public fun String.removeInvisibleChars(): String = replace(Regex("[\\p{C}\\p{Z}&&[^\\p{Zs}]]"), "")
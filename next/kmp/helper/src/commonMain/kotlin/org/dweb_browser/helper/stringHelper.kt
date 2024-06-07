package org.dweb_browser.helper

fun String.removeInvisibleChars() = replace(Regex("[\\p{C}\\p{Z}&&[^\\p{Zs}]]"), "")
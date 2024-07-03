package org.dweb_browser.helper

import io.ktor.http.decodeURLPart
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent

fun String.encodeURIComponent(): String = this.encodeURLQueryComponent()
fun String.decodeURIComponent(): String = this.decodeURLQueryComponent()
fun String.encodeURI(): String = this.encodeURLPath()
fun String.decodeURI(): String = this.decodeURLPart()
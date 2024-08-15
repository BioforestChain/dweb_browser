package org.dweb_browser.helper

import io.ktor.http.decodeURLPart
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent

public fun String.encodeURIComponent(): String = this.encodeURLQueryComponent()
public fun String.decodeURIComponent(): String = this.decodeURLQueryComponent()
public fun String.encodeURI(): String = this.encodeURLPath()
public fun String.decodeURI(): String = this.decodeURLPart()
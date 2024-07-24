package org.dweb_browser.pure.http.ext

import io.ktor.http.ContentType

val ContentType.mime get() = "$contentType/$contentSubtype"
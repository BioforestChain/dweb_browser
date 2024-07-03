package org.dweb_browser.helper

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
val ByteArray.base64String: String get() = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
val ByteArray.base64UrlString: String get() = Base64.UrlSafe.encode(this)

@OptIn(ExperimentalEncodingApi::class)
val String.base64Binary get() = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
val String.base64UrlBinary get() = Base64.UrlSafe.decode(this)

package org.dweb_browser.helper

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
public val ByteArray.base64String: String get() = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
public val ByteArray.base64UrlString: String get() = Base64.UrlSafe.encode(this)

@OptIn(ExperimentalEncodingApi::class)
public val String.base64Binary: ByteArray get() = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
public val String.base64UrlBinary: ByteArray get() = Base64.UrlSafe.decode(this)

public val String.utf8ToBase64String: String get() = utf8Binary.base64String

public val String.utf8ToBase64UrlString: String get() = utf8Binary.base64UrlString

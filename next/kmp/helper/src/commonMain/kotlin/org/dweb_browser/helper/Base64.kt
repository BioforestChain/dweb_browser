package org.dweb_browser.helper

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

val ByteArray.utf8 get() = decodeToString()
val String.bytearray get() = encodeToByteArray()

@OptIn(ExperimentalEncodingApi::class)
val ByteArray.base64: String get() = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
val ByteArray.base64Url: String get() = Base64.UrlSafe.encode(this)

@OptIn(ExperimentalEncodingApi::class)
val String.asBase64 get() = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
val String.asBase64Url get() = Base64.UrlSafe.decode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64ToByteArray(): ByteArray = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64UrlToByteArray(): ByteArray = Base64.UrlSafe.decode(this)
package org.dweb_browser.helper

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256

private val sha256 = CryptographyProvider.Default.get(SHA256)
suspend fun sha256(data: ByteArray) = sha256.hasher().hash(data)
suspend fun sha256(data: String) = sha256(data.toUtf8ByteArray())

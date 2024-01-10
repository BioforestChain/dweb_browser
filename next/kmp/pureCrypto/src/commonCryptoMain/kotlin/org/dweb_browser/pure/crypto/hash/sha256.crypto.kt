package org.dweb_browser.pure.crypto.hash

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256

private val sha256Provider = CryptographyProvider.Default.get(SHA256)
suspend fun sha256Common(data: ByteArray) = sha256Provider.hasher().hash(data)
//suspend fun sha256(data: String) = sha256(data.toUtf8ByteArray())
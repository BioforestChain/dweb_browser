package org.dweb_browser.pure.crypto.hash

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256

private val sha256Provider = CryptographyProvider.Default.get(SHA256)
suspend fun common_sha256(data: ByteArray) = sha256Provider.hasher().hash(data)
suspend fun common_sha256(data: String) = sha256Provider.hasher().hash(data.encodeToByteArray())

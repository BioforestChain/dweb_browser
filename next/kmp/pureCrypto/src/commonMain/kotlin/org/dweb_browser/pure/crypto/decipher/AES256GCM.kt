package org.dweb_browser.pure.crypto.decipher


expect suspend fun decipher_aes_256_gcm(key: ByteArray, data: ByteArray): ByteArray
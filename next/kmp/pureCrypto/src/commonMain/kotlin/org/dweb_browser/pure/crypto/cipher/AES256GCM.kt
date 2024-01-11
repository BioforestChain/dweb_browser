package org.dweb_browser.pure.crypto.cipher


expect suspend fun cipher_aes_256_gcm(key: ByteArray, data: ByteArray): ByteArray
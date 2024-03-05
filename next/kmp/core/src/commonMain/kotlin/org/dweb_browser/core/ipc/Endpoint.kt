package org.dweb_browser.core.ipc

/**
 *
 */
abstract class Gateway {

  abstract suspend fun send()

  abstract suspend fun receive()

  abstract suspend fun fork(): Gateway
}
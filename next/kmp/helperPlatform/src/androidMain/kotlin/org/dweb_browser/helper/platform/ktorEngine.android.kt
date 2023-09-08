package org.dweb_browser.helper.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.server.engine.ApplicationEngineFactory

actual fun getKtorClientEngine(): HttpClientEngineFactory<*> = io.ktor.client.engine.cio.CIO
actual fun getKtorServerEngine(): ApplicationEngineFactory<*, *> = io.ktor.server.cio.CIO

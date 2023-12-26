package org.dweb_browser.pure.http.engine

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.server.engine.ApplicationEngineFactory

expect fun getKtorClientEngine(): HttpClientEngineFactory<*>
expect fun getKtorServerEngine(): ApplicationEngineFactory<*, *>

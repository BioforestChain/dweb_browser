package org.dweb_browser.helper.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.server.engine.ApplicationEngineFactory

expect fun getKtorClientEngine(): HttpClientEngineFactory<*>
expect fun getKtorServerEngine(): ApplicationEngineFactory<*, *>

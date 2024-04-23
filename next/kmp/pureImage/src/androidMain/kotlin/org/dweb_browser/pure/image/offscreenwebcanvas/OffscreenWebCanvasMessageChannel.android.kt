package org.dweb_browser.pure.image.offscreenwebcanvas

import org.dweb_browser.pure.http.HttpPureServer

internal actual suspend fun startPureServer(server: HttpPureServer) = commonStartPureServer(server)

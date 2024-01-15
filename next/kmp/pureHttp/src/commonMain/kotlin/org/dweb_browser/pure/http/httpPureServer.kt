package org.dweb_browser.pure.http

typealias HttpPureServerOnRequest = suspend (PureServerRequest) -> PureResponse

expect suspend fun httpPureServer(onRequest: HttpPureServerOnRequest, port: UShort): UShort
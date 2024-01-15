package org.dweb_browser.pure.http

expect suspend fun httpPureClient(request: PureClientRequest): PureResponse

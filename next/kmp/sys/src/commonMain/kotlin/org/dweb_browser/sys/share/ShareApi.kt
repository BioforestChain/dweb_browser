package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData

expect suspend fun share(shareOptions: ShareOptions, multiPartData: MultiPartData?): String
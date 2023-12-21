package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import org.dweb_browser.core.module.NativeMicroModule

expect suspend fun share(shareOptions: ShareOptions, multiPartData: MultiPartData?): String

expect suspend fun share(shareOptions: ShareOptions, files: List<String>, shareNMM: NativeMicroModule? = null): String
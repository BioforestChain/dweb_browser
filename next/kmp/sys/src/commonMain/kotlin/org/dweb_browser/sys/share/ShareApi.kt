package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import org.dweb_browser.core.module.MicroModule

expect suspend fun share(
  shareOptions: ShareOptions,
  multiPartData: MultiPartData?,
  shareNMM: MicroModule.Runtime,
): String

expect suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: MicroModule.Runtime,
): String
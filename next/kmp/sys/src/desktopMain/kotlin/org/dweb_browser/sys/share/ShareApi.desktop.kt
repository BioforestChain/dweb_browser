package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import org.dweb_browser.core.module.MicroModule

actual suspend fun share(
  shareOptions: ShareOptions,
  multiPartData: MultiPartData?,
  shareNMM: MicroModule.Runtime,
): String {
  TODO("Not yet implemented share with multiPartData")
}

actual suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: MicroModule.Runtime,
): String {
  TODO("Not yet implemented share with files")
}
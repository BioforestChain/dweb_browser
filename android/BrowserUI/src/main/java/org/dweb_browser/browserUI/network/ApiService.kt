package org.dweb_browser.browserUI.network

import java.io.File

interface ApiService {
  suspend fun getNetWorker(url: String): String

  suspend fun downloadAndSave(
    url: String,
    file: File,
    total: Long,
    isStop: () -> Boolean,
    onProgress: suspend (Long, Long) -> Unit
  )

  suspend fun breakpointDownloadAndSave(
    url: String,
    file: File,
    total: Long,
    isStop: () -> Boolean,
    onProgress: suspend (Long, Long) -> Unit
  )

  companion object {
    val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      ApiServiceImpl()
    }
  }
}

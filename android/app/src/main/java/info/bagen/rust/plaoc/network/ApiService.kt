package info.bagen.rust.plaoc.network

import info.bagen.rust.plaoc.network.base.ApiResultData
import info.bagen.rust.plaoc.network.base.BaseData
import info.bagen.rust.plaoc.ui.entity.AppVersion
import java.io.File

interface ApiService {

  suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>>

  suspend fun getNetWorker(url: String): String

  suspend fun downloadAndSave(
    path: String, file: File?, isStop: () -> Boolean, DLProgress: (Long, Long) -> Unit
  )

  suspend fun breakpointDownloadAndSave(
    path: String,
    file: File?,
    total: Long,
    isStop: () -> Boolean,
    DLProgress: (Long, Long) -> Unit
  )

  companion object {
    val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      ApiServiceImpl(HttpClient())
    }
  }
}

package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.ext.cancelDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.downloadProgressFlow
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.pickFile
import org.dweb_browser.core.std.file.ext.readFile
import org.dweb_browser.core.std.file.ext.removeFile
import org.dweb_browser.core.std.file.ext.writeFile
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.valueIn
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController

class JmmController(private val jmmNMM: JmmNMM.JmmRuntime, private val jmmStore: JmmStore) {
  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  // 构建jmm历史记录
  val historyMetadataMaps: MutableMap<String, JmmMetadata> = mutableStateMapOf()

  // 构建历史的controller
  private val historyController = JmmHistoryController(jmmNMM, this)

  // 打开历史界面
  suspend fun openHistoryView(win: WindowController) = historyController.showHistoryView(win)

  suspend fun loadHistoryMetadataUrl() {
    val loadMap = jmmStore.getAllMetadata()
    historyMetadataMaps.clear()
    if (loadMap.filter { (key, value) -> key != value.metadata.id }.isNotEmpty()) {
      // 为了替换掉旧数据，旧数据使用originUrl来保存的，现在改为mmid，add by 240201
      val saveMap = mutableMapOf<String, MutableList<JmmMetadata>>()
      loadMap.forEach { (_, value) ->
        saveMap.getOrPut(value.metadata.id) { mutableListOf() }.add(value)
      }
      jmmStore.clearMetadata() // 先删除旧的，然后再重新插入新的
      saveMap.forEach { (key, list) ->
        list.sortByDescending { it.metadata.version.replace(".", "0").toLong() }
        list.firstOrNull()?.let { jmmStore.saveMetadata(key, it) } // 取最后新的版本进行保存
      }
      historyMetadataMaps.putAll(jmmStore.getAllMetadata()) // 重新加载最新数据
    } else {
      historyMetadataMaps.putAll(loadMap)
    }
    historyMetadataMaps.forEach { (key, historyMetadata) ->
      if (historyMetadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
        // 获取下载的进度，如果进度 >= 0 表示有下载
        val current = historyMetadata.taskId?.let { jmmNMM.currentDownload(it) } ?: -1L
        historyMetadata.state = if (current >= 0L) {
          historyMetadata.state.copy(state = JmmStatus.Paused, current = current)
        } else {
          historyMetadata.state.copy(state = JmmStatus.Init)
        }
        jmmStore.saveMetadata(key, historyMetadata)
      } else if (jmmNMM.bootstrapContext.dns.query(historyMetadata.metadata.id) == null) {
        historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Init) // 如果没有找到，说明被卸载了
        jmmStore.saveMetadata(key, historyMetadata)
      }
    }
  }

  private val installViews = SafeHashMap<String, JmmInstallerController>()

  // 记录旧的版本
  private var oldVersion: String? = null

  /**打开bottomSheet的详情页面*/
  suspend fun openBottomSheet(
    metadata: JmmMetadata
  ): JmmInstallerController {
    val installerController = installViews.getOrPut(metadata.metadata.id) {
      JmmInstallerController(
        jmmNMM = jmmNMM, metadata = metadata, jmmController = this@JmmController
      )
    }
    installerController.openRender()
    return installerController
  }

  /**
   * 显示应用安装界面时，需要最新的 AppInstallManifest 数据
   */
  suspend fun fetchJmmMetadata(metadataUrl: String): JmmMetadata {
    val response = jmmNMM.nativeFetch(metadataUrl)
    if (!response.isOk) {
      throw ResponseException(code = response.status)
    }
    val manifest = response.json<JmmAppInstallManifest>()

    val baseURI = when (manifest.baseURI?.isWebUrl()) {
      true -> manifest.baseURI!!
      else -> when (val baseUri = manifest.baseURI) {
        null -> metadataUrl
        else -> buildUrlString(metadataUrl) { resolvePath(baseUri) }
      }.also { uri ->
        manifest.baseURI = uri
      }
    }
    // 如果bundle_url没有host
    if (!manifest.bundle_url.isWebUrl()) {
      manifest.bundle_url =
        baseURI.replace("metadata.json", manifest.bundle_url.substring(2))
    }
    return manifest.createJmmMetadata(metadataUrl)
  }

  /**
   * 打开安装器视图
   * @originUrl  元数据地址
   * @openHistoryMetadata 元数据
   * @fromHistory 是否是
   */
  suspend fun openInstallerView(newMetadata: JmmMetadata) {
    debugJMM("openInstallerView", newMetadata.metadata.bundle_url)
    compareLocalMetadata(newMetadata)
    val installerController = openBottomSheet(newMetadata)
    // 不管是否替换的，都进行一次存储新状态，因为需要更新下载状态
    installerController.installMetadata = newMetadata
  }

  /**
   * 对比本地已经存在的数据，从而更新这个 JmmMetadata 的一些相关状态。
   * 并按需触发数据库保存
   **/
  private suspend fun compareLocalMetadata(
    newMetadata: JmmMetadata,
    save: Boolean = true
  ) {
    val mmid = newMetadata.metadata.id
    // 拿到已经安装过的准备对比
    val oldMM = jmmNMM.bootstrapContext.dns.query(mmid)
    val oldMetadata = historyMetadataMaps[mmid]
    debugJMM("compareLocalMetadata") {
      "installMM=${oldMetadata?.metadata?.version} mmid=$mmid"
    }
    // 从未安装过，直接替换成当前的，不考虑是否比历史列表高
    if (oldMetadata == null || oldMM == null) {
      if (save) {
        saveMetadata(newMetadata)
      }
      return
    }

    val oldManifest = oldMetadata.metadata
    val newManifest = newMetadata.metadata
    // 比安装高，直接进行替换
    if (newManifest.version.isGreaterThan(oldManifest.version)) {
      oldVersion = oldMetadata.metadata.version
      // session 处理
      val session = getAppSessionInfo(newManifest.id, oldMetadata.metadata.version)
      debugJMM("openInstallerView", "is order app and session=$session")
      // 如果列表的应用是下载中的，那么需要移除掉
      if (oldMetadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
        oldMetadata.taskId?.let { taskId -> jmmNMM.cancelDownload(taskId) }
      }

      newMetadata.state = JmmStatusEvent(state = JmmStatus.NewVersion)
      newMetadata.installTime = session?.installTime ?: datetimeNow()
      if (save) {
        saveMetadata(newMetadata)
      }
      return
    }
    // 版本相同
    if (newManifest.version == oldManifest.version) {
      // 这里表示二者是一样的，此时new状态需要跟old保持一致，而不是直接置为 Installed
      newMetadata.state = JmmStatusEvent(
        current = oldMetadata.state.current,
        total = oldMetadata.state.total,
        state = oldMetadata.state.state
      )
    } else { // 比安装的应用版本还低的，直接不能安装，提示版本过低，不存储
      newMetadata.state = JmmStatusEvent(state = JmmStatus.VersionLow)
    }
  }

  /**只有需要存储的时候才存起来*/
  private suspend fun saveMetadata(differentMetadata: JmmMetadata) {
    debugJMM("saveMetadata", differentMetadata)
    historyMetadataMaps[differentMetadata.metadata.id] = differentMetadata
    jmmStore.saveMetadata(
      differentMetadata.metadata.id, differentMetadata
    )
  }

  suspend fun uninstall(mmid: MMID): Boolean {
    val data = jmmStore.getApp(mmid) ?: return false
    // 在dns中移除app
    jmmNMM.bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除整个app
    jmmNMM.removeFile("/data/apps/${mmid}-${data.installManifest.version}")
    // 从磁盘中移除整个
    jmmStore.deleteApp(mmid)
    historyMetadataMaps[mmid]?.initState(jmmStore) // 恢复成Init状态
    return true
  }

  private suspend fun watchProcess(taskId: String, metadata: JmmMetadata): Boolean {
    var success = false;
    jmmNMM.downloadProgressFlow(taskId).collect { status ->
      if (status.state == DownloadState.Completed) {
        success = true
      }
      metadata.updateDownloadStatus(status, jmmStore)
    }
    return success
  }

  private val downloadLock = Mutex()


  suspend fun startDownloadTaskByUrl(originUrl: String) {
    val metadata = fetchJmmMetadata(originUrl)
    compareLocalMetadata(metadata)
    when (val state = metadata.state.state) {
      JmmStatus.Init -> startDownloadTask(metadata)
      else -> debugJMM("startDownloadTaskByUrl", "fail to start state=$state url=$originUrl")
    }
  }

  /**
   * 创建任务并下载，可以判断 taskId 在 download 中是否存在，如果不存在就创建，存在直接下载
   */
  suspend fun startDownloadTask(metadata: JmmMetadata): Unit = downloadLock.withLock {
    var taskId = metadata.taskId
    if (taskId == null || !jmmNMM.existsDownload(taskId)) {
      val downloadTask = with(metadata.metadata) {
        jmmNMM.createDownloadTask(url = bundle_url, total = bundle_size)
      }
      metadata.initDownloadTask(downloadTask, jmmStore)
      debugJMM("JmmController", "createAndStartDownloadTask => $metadata")
      /// 监听
      taskId = downloadTask.id
      jmmNMM.scopeLaunch(cancelable = true) {
        if (watchProcess(taskId, metadata)) {
          if (decompress(downloadTask, metadata)) {
            jmmNMM.bootstrapContext.dns.uninstall(metadata.metadata.id)
            jmmNMM.bootstrapContext.dns.install(JsMicroModule(metadata.metadata))
            metadata.installComplete(jmmStore)
          } else {
            jmmNMM.showToast(BrowserI18nResource.toast_message_download_unzip_fail.text)
            metadata.installFail(jmmStore)
          }
          // 删除缓存的zip文件
          jmmNMM.removeFile(downloadTask.filepath)
          // 更新完需要删除旧的app版本，这里如果有保存用户数据需要一起移动过去，但是现在这里是单纯的删除
          if (oldVersion != null) {
            jmmNMM.removeFile("/data/apps/${metadata.metadata.id}-${oldVersion}")
            oldVersion = null
          }
        }
      }
      /// 开始
      jmmNMM.startDownload(taskId).falseAlso {
        jmmNMM.showToast(BrowserI18nResource.toast_message_download_download_fail.text)
      }
    } else if (metadata.state.state == JmmStatus.Downloading) {
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_downloading.text)
    } else if (metadata.state.state == JmmStatus.Paused) {
      jmmNMM.startDownload(taskId).falseAlso {
        jmmNMM.showToast(BrowserI18nResource.toast_message_download_download_fail.text)
      }
    } else {
    }
  }

  suspend fun pause(metadata: JmmMetadata) = metadata.taskId?.let { taskId ->
    jmmNMM.pauseDownload(taskId)
  } ?: false

  private suspend fun decompress(
    task: DownloadTask,
    jmmMetadata: JmmMetadata,
  ): Boolean {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    val sourcePath = task.filepath

    // 用于校验jmmApp下载文件是不是完整
    if (!jmmAppHashVerify(jmmNMM, jmmMetadata, sourcePath)) {
      debugJMM("decompress", "校验失败")
      return false
    }

    val targetPath = jmmNMM.pickFile("/data/apps/$jmm")
    val decompressRes = jmmNMM.nativeFetch(buildUrlString("file://zip.browser.dweb/decompress") {
      parameters.append("sourcePath", sourcePath)
      parameters.append("targetPath", targetPath)
    })
    debugJMM("decompress") { "$jmm ok:${decompressRes.isOk}"}
    return decompressRes.isOk.trueAlso {
      // 保存 session（记录安装时间） 和 metadata （app数据源）
      jmmNMM.writeFile(
        path = "/data/apps/$jmm/usr/sys/metadata.json",
        body = IPureBody.from(Json.encodeToString(jmmMetadata.metadata))
      )
      debugJMM("decompress") { "installTime=${jmmMetadata.installTime} installUrl:${jmmMetadata.originUrl}"}
      jmmNMM.writeFile(
        path = "/data/apps/$jmm/usr/sys/session.json",
        body = IPureBody.from(Json.encodeToString(buildJsonObject {
          put("installTime", JsonPrimitive(jmmMetadata.installTime))
          put("updateTime", JsonPrimitive(datetimeNow()))
          put("installUrl", JsonPrimitive(jmmMetadata.originUrl))
        }))
      )
    }.falseAlso {
      debugJMM("decompress", "解压失败", "${decompressRes.status} ${decompressRes.text()}")
    }
  }

  /**尝试获取app session 这个可以保证更新的时候数据不被清空*/
  private suspend fun getAppSessionInfo(mmid: MMID, version: String): SessionInfo? {
    val session = jmmNMM.readFile("/data/apps/${mmid}-${version}/usr/sys/session.json")
    return if (session.isOk) {
      Json.decodeFromString<SessionInfo>(
        session.text()
      )
    } else {
      null
    }
  }

  suspend fun removeHistoryMetadata(historyMetadata: JmmMetadata) {
    historyMetadataMaps.remove(historyMetadata.metadata.id)
    historyMetadata.taskId?.let { taskId -> jmmNMM.removeDownload(taskId) }
    jmmStore.deleteMetadata(historyMetadata.metadata.id)
  }
}

@Serializable
data class SessionInfo(val installTime: Long, val updateTime: Long, val installUrl: String)

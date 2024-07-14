package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateMapOf
import io.ktor.http.HttpStatusCode
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
import org.dweb_browser.browser.download.ext.downloadProgressFlow
import org.dweb_browser.browser.download.ext.existDownloadTask
import org.dweb_browser.browser.download.ext.getDownloadTask
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
import org.dweb_browser.helper.SuspendOnceWithKey
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.valueIn
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.WindowController

class JmmController(private val jmmNMM: JmmNMM.JmmRuntime, private val jmmStore: JmmStore) {

  // 构建jmm历史记录
  val historyMetadataMaps: MutableMap<String, JmmMetadata> = mutableStateMapOf()

  // 构建历史的controller
  private val renderController = JmmRenderController(jmmNMM, this)

  // 打开历史界面
  suspend fun openHistoryView(win: WindowController) = renderController.showView(win)

  suspend fun loadHistoryMetadataUrl() {
    val loadMap = jmmStore.getAllHistory()
    historyMetadataMaps.clear()
    if (loadMap.filter { (key, value) -> key != value.manifest.id }.isNotEmpty()) {
      // 为了替换掉旧数据，旧数据使用originUrl来保存的，现在改为mmid，add by 240201
      val saveMap = mutableMapOf<String, MutableList<JmmMetadata>>()
      loadMap.forEach { (_, value) ->
        saveMap.getOrPut(value.manifest.id) { mutableListOf() }.add(value)
      }
      jmmStore.clearHistory() // 先删除旧的，然后再重新插入新的
      saveMap.forEach { (key, list) ->
        list.sortByDescending { it.manifest.version.replace(".", "0").toLong() }
        list.firstOrNull()?.let { jmmStore.saveHistory(key, it) } // 取最后新的版本进行保存
      }
      historyMetadataMaps.putAll(jmmStore.getAllHistory()) // 重新加载最新数据
    } else {
      historyMetadataMaps.putAll(loadMap)
    }
    historyMetadataMaps.forEach { (key, historyMetadata) ->
      if (historyMetadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
        // 获取下载的进度，如果进度 >= 0 表示有下载
        historyMetadata.downloadTask?.let { jmmNMM.getDownloadTask(it.id) }?.let { downloadTask ->
          historyMetadata.initDownloadTask(downloadTask, jmmStore)
          if (downloadTask.status.state == DownloadState.Completed) { // 如果是完成了，那么考虑直接做解压
            jmmNMM.scopeLaunch(cancelable = true) {
              decompress(downloadTask, historyMetadata)
            }
          }
        } ?: run {
          historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Init, current = 0L)
          jmmStore.saveHistory(key, historyMetadata)
        }
      } else if (jmmNMM.bootstrapContext.dns.query(historyMetadata.manifest.id) == null) {
        historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Init) // 如果没有找到，说明被卸载了
        jmmStore.saveHistory(key, historyMetadata)
      }
    }
  }

  private val installViews = SafeHashMap<String, JmmDetailController>()

  // 记录旧的版本
  private var oldVersion: String? = null

  fun getInstallerController(metadata: JmmMetadata) = installViews.getOrPut(metadata.manifest.id) {
    JmmDetailController(
      jmmNMM = jmmNMM, metadata = metadata, jmmController = this@JmmController
    )
  }.also {
    // 不管是否替换的，都进行一次存储新状态，因为需要更新下载状态
    it.metadata = metadata
  }

  /**打开bottomSheet的详情页面*/
  suspend fun openBottomSheet(metadata: JmmMetadata) = getInstallerController(metadata).also {
    it.openBottomSheet()
  }

  /**
   * 显示应用安装界面时，需要最新的 AppInstallManifest 数据
   */
  suspend fun fetchJmmMetadata(metadataUrl: String, referrerUrl: String?): JmmMetadata {
    val response = jmmNMM.nativeFetch(metadataUrl)
    if (!response.isOk) {
      throw ResponseException(code = response.status, "fail to fetch metadataUrl: $metadataUrl")
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
      manifest.bundle_url = baseURI.replace("metadata.json", manifest.bundle_url.substring(2))
    }
    return manifest.createJmmMetadata(metadataUrl, referrerUrl, JmmStatus.Init)
  }

  /**
   * 打开安装器视图
   */
  suspend fun openInstallerView(jmmMetadata: JmmMetadata) {
    debugJMM("openInstallerView", jmmMetadata.manifest.bundle_url)
    compareLocalMetadata(jmmMetadata, save = true)
    getInstallerController(jmmMetadata).openBottomSheet()
  }

  /**
   * 打开详情页视图
   */
  fun openDetailView(jmmMetadata: JmmMetadata) {
    debugJMM("openDetailView", jmmMetadata.manifest.bundle_url)
    renderController.openDetail(jmmMetadata)
  }

  /**
   * 对比本地已经存在的数据，从而更新这个 JmmMetadata 的一些相关状态。
   * 并按需触发数据库保存
   **/
  private suspend fun compareLocalMetadata(newMetadata: JmmMetadata, save: Boolean) {
    val mmid = newMetadata.manifest.id
    // 拿到已经安装过的准备对比
    val oldMM = jmmNMM.bootstrapContext.dns.query(mmid)
    val oldMetadata = jmmStore.getApp(mmid)?.jmmMetadata
    debugJMM("compareLocalMetadata") {
      "installMM=${oldMetadata?.manifest?.version} mmid=$mmid"
    }
    // 从未安装过，直接替换成当前的，不考虑是否比历史列表高
    if (oldMetadata == null || oldMM == null) {
      if (save) {
        saveMetadata(newMetadata)
      }
      return
    }

    val oldManifest = oldMetadata.manifest
    val newManifest = newMetadata.manifest
    // 比安装高，直接进行替换
    if (newManifest.version.isGreaterThan(oldManifest.version) ||
      // 如果老版本处于内核非兼容的状态，
      (!oldManifest.canSupportTarget(JsMicroModule.VERSION)
          // 而新版本有做了兼容内核的升级，那么也属于版本升级
          && newManifest.canSupportTarget(JsMicroModule.VERSION))
    ) {
      oldVersion = oldMetadata.manifest.version
      // session 处理
      val session = getAppSessionInfoCompact(newManifest.id, oldMetadata.manifest.version)
      debugJMM("openInstallerView", "is order app and session=$session")
      // 如果列表的应用是下载中的，那么需要移除掉
      if (oldMetadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
        oldMetadata.downloadTask?.id?.let { taskId -> jmmNMM.cancelDownload(taskId) }
      }

      newMetadata.state = newMetadata.state.copy(state = JmmStatus.NewVersion)
      newMetadata.installTime = session?.installTime ?: datetimeNow()
      if (save) {
        saveMetadata(newMetadata)
      }
      return
    }
    // 版本相同
    if (newManifest.version == oldManifest.version) {
      // 这里表示二者是一样的，此时new状态需要跟old保持一致，而不是直接置为 Installed
      newMetadata.state = newMetadata.state.copy(
        current = oldMetadata.state.current,
        total = oldMetadata.state.total,
        state = oldMetadata.state.state
      )
    } else { // 比安装的应用版本还低的，直接不能安装，提示版本过低，不存储
      newMetadata.state = newMetadata.state.copy(state = JmmStatus.VersionLow, current = 0L)
    }
  }

  /**只有需要存储的时候才存起来*/
  private suspend fun saveMetadata(differentMetadata: JmmMetadata) {
    debugJMM("saveMetadata", differentMetadata)
    historyMetadataMaps[differentMetadata.manifest.id] = differentMetadata
    jmmStore.saveHistory(
      differentMetadata.manifest.id, differentMetadata
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

  private val downloadLock = Mutex()

  suspend fun startDownloadTaskByUrl(originUrl: String, referrerUrl: String?) = try {
    val metadata = fetchJmmMetadata(originUrl, referrerUrl)
    compareLocalMetadata(metadata, save = true)
    when (val state = metadata.state.state) {
      JmmStatus.Init -> startDownloadTask(metadata)
      else -> debugJMM("startDownloadTaskByUrl", "fail to start state=$state url=$originUrl")
    }
  } catch (e: ResponseException) {
    val message = if (e.code == HttpStatusCode.NotFound) {
      JmmI18nResource.url_invalid.text
    } else {
      "${e.code.value} >> ${e.code.description}"
    }
    jmmNMM.showToast(message = message)
  }

  private suspend fun decompressProcess(downloadTask: DownloadTask, metadata: JmmMetadata) {
    if (decompress(downloadTask, metadata)) {
      jmmNMM.bootstrapContext.dns.uninstall(metadata.manifest.id)
      jmmNMM.bootstrapContext.dns.install(JsMicroModule(metadata.manifest))
      metadata.installComplete(jmmStore)
    } else {
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_unzip_fail.text)
      metadata.installFail(jmmStore)
    }
    // 删除缓存的zip文件
    jmmNMM.removeFile(downloadTask.filepath)
    // 更新完需要删除旧的app版本，这里如果有保存用户数据需要一起移动过去，但是现在这里是单纯的删除
    if (oldVersion != null && oldVersion != metadata.manifest.version) {
      jmmNMM.removeFile("/data/apps/${metadata.manifest.id}-${oldVersion}")
      oldVersion = null
    }
  }

  private val onceDownload = SuspendOnceWithKey(jmmNMM.getRuntimeScope())

  private suspend fun watchDownloadProcess(metadata: JmmMetadata) {
    val downloadTask = metadata.downloadTask ?: run {
      debugJMM("watchDownloadProcess", "downloadTask is null")
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_unzip_fail.text)
      return
    }
    onceDownload.executeOnce(downloadTask.id) {
      debugJMM("watchDownloadProcess", "key=${downloadTask.id}")
      var success = false
      jmmNMM.downloadProgressFlow(downloadTask.id).collect { status ->
        if (status.state == DownloadState.Completed) {
          success = true
        }
        metadata.updateDownloadStatus(status, jmmStore)
      }
      if (success) {
        decompressProcess(downloadTask, metadata)
      }
    }
  }

  /**
   * 创建任务并下载，可以判断 taskId 在 download 中是否存在，如果不存在就创建，存在直接下载
   */
  suspend fun startDownloadTask(metadata: JmmMetadata): Unit = downloadLock.withLock {
    val taskId = metadata.downloadTask?.id
    if (taskId == null || !jmmNMM.existDownloadTask(taskId)) {
      val downloadTask = with(metadata.manifest) {
        jmmNMM.createDownloadTask(url = bundle_url, total = bundle_size)
      }
      metadata.initDownloadTask(downloadTask, jmmStore)
      debugJMM("JmmController", "createAndStartDownloadTask => $metadata")
      /// 监听
      watchDownloadProcess(metadata)
      /// 开始
      jmmNMM.startDownload(downloadTask.id).falseAlso {
        jmmNMM.showToast(BrowserI18nResource.toast_message_download_download_fail.text)
      }
    } else if (metadata.state.state == JmmStatus.Downloading) {
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_downloading.text)
    } else if (metadata.state.state.valueIn(JmmStatus.Paused)) {
      /// 监听
      watchDownloadProcess(metadata)
      jmmNMM.startDownload(taskId).falseAlso {
        jmmNMM.showToast(BrowserI18nResource.toast_message_download_download_fail.text)
      }
    }
  }

  suspend fun pause(metadata: JmmMetadata) = metadata.downloadTask?.id?.let { taskId ->
    val status = jmmNMM.pauseDownload(taskId)
    metadata.updateDownloadStatus(status, jmmStore)
  }

  private suspend fun decompress(
    task: DownloadTask,
    jmmMetadata: JmmMetadata,
  ): Boolean {
    val dirName = task.url.substring(task.url.lastIndexOf("/") + 1).let { zipName ->
      zipName.substring(0, zipName.lastIndexOf("."))
    }
    val mmid = jmmMetadata.manifest.id
    val sourcePath = task.filepath

    // 用于校验jmmApp下载文件是不是完整
    if (!jmmAppHashVerify(jmmNMM, jmmMetadata, sourcePath)) {
      debugJMM("decompress", "校验失败")
      return false
    }

    val targetPath = jmmNMM.pickFile("/data/apps/$dirName")
    val decompressRes = jmmNMM.nativeFetch(buildUrlString("file://zip.browser.dweb/decompress") {
      parameters.append("sourcePath", sourcePath)
      parameters.append("targetPath", targetPath)
    })
    debugJMM("decompress") { "$dirName ok:${decompressRes.isOk}" }
    return decompressRes.isOk.trueAlso {
      // 保存 session（记录安装时间） 和 metadata （app数据源）
      Json.encodeToString(jmmMetadata.manifest).also { manifestJson ->
        jmmNMM.writeFile(
          path = "/data/apps/$dirName/usr/sys/metadata.json", body = IPureBody.from(manifestJson)
        )
        jmmNMM.writeFile(
          path = "/data/app-data/${mmid}/metadata.json", body = IPureBody.from(manifestJson)
        )
      }
      debugJMM("decompress") { "installTime=${jmmMetadata.installTime} installUrl:${jmmMetadata.originUrl}" }
      Json.encodeToString(buildJsonObject {
        put("installTime", JsonPrimitive(jmmMetadata.installTime))
        put("updateTime", JsonPrimitive(datetimeNow()))
        put("installUrl", JsonPrimitive(jmmMetadata.originUrl))
      }).also { sessionJson ->
        jmmNMM.writeFile(
          path = "/data/apps/$dirName/usr/sys/session.json", body = IPureBody.from(sessionJson)
        )
        jmmNMM.writeFile(
          path = "/data/app-data/$mmid/session.json", body = IPureBody.from(sessionJson)
        )
      }
    }.falseAlso {
      debugJMM("decompress", "解压失败", "${decompressRes.status} ${decompressRes.text()}")
    }
  }

  /**尝试获取app session 这个可以保证更新的时候数据不被清空*/
  private suspend fun getAppSessionInfoCompact(mmid: MMID, version: String): SessionInfo? {
    return getAppSessionInfo(mmid) ?: getAppSessionInfoOld(mmid, version)
  }

  /**尝试获取app session 这个可以保证更新的时候数据不被清空*/
  private suspend fun getAppSessionInfo(mmid: MMID): SessionInfo? {
    return jmmNMM.readFile("/data/app-data/${mmid}/session.json").jsonOrNull<SessionInfo>()
  }


  /**尝试获取app session 这个可以保证更新的时候数据不被清空*/
  private suspend fun getAppSessionInfoOld(mmid: MMID, version: String): SessionInfo? {
    return jmmNMM.readFile("/data/apps/${mmid}-${version}/usr/sys/session.json")
      .jsonOrNull<SessionInfo>()
  }


  suspend fun removeHistoryMetadata(historyMetadata: JmmMetadata) {
    historyMetadataMaps.remove(historyMetadata.manifest.id)
    historyMetadata.downloadTask?.id?.let { taskId -> jmmNMM.removeDownload(taskId) }
    jmmStore.deleteHistory(historyMetadata.manifest.id)
  }

  suspend fun openApp(mmid: MMID) {
    jmmNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=$mmid")
  }
}

@Serializable
data class SessionInfo(val installTime: Long, val updateTime: Long, val installUrl: String)

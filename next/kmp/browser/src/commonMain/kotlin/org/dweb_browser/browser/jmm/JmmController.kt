package org.dweb_browser.browser.jmm

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.ext.cancelDownload
import org.dweb_browser.browser.download.ext.createChannelOfDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.removeDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.pickFile
import org.dweb_browser.core.std.file.ext.readFile
import org.dweb_browser.core.std.file.ext.removeFile
import org.dweb_browser.core.std.file.ext.writeFile
import org.dweb_browser.helper.LateInit
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

class JmmController(private val jmmNMM: JmmNMM, private val jmmStore: JmmStore) {
  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  // 构建jmm历史记录
  val historyMetadataMaps: MutableMap<String, JmmHistoryMetadata> = mutableStateMapOf()

  // 构建历史的controller
  private val historyController = JmmHistoryController(jmmNMM, this)

  // 打开历史界面
  suspend fun openHistoryView(win: WindowController) = historyController.openHistoryView(win)

  suspend fun loadHistoryMetadataUrl() {
    val loadMap = jmmStore.getAllHistoryMetadata()
    historyMetadataMaps.clear()
    if (loadMap.filter { (key, value) -> key != value.metadata.id }.isNotEmpty()) {
      // 为了替换掉旧数据，旧数据使用originUrl来保存的，现在改为mmid，add by 240201
      val saveMap = mutableMapOf<String, MutableList<JmmHistoryMetadata>>()
      loadMap.forEach { (_, value) ->
        saveMap.getOrPut(value.metadata.id) { mutableListOf() }.add(value)
      }
      jmmStore.clearHistoryMetadata() // 先删除旧的，然后再重新插入新的
      saveMap.forEach { (key, list) ->
        list.sortByDescending { it.metadata.version.replace(".", "0").toLong() }
        list.firstOrNull()?.let { jmmStore.saveHistoryMetadata(key, it) } // 取最后新的版本进行保存
      }
      historyMetadataMaps.putAll(jmmStore.getAllHistoryMetadata()) // 重新加载最新数据
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
        jmmStore.saveHistoryMetadata(key, historyMetadata)
      } else if (jmmNMM.bootstrapContext.dns.query(historyMetadata.metadata.id) == null) {
        historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Init) // 如果没有找到，说明被卸载了
        jmmStore.saveHistoryMetadata(key, historyMetadata)
      }
    }
  }

  private val installViews = SafeHashMap<String, LateInit<JmmInstallerController>>()

  // 记录旧的版本
  private var oldVersion: String? = null

  /**
   * 打开安装器视图
   */
  suspend fun openOrUpsetInstallerView(
    originUrl: String, openHistoryMetadata: JmmHistoryMetadata? = null, fromHistory: Boolean = false
  ) = installViews.getOrPut(originUrl) {
    LateInit()
  }.let {
    debugJMM("openInstallerView", "$originUrl, $fromHistory, $openHistoryMetadata")
    val jmmInstallerController = it.getOrInit {
      JmmInstallerController(
        jmmNMM = jmmNMM,
        jmmHistoryMetadata = openHistoryMetadata
          ?: JmmAppInstallManifest().createJmmHistoryMetadata(originUrl),
        jmmController = this@JmmController,
        openFromHistory = fromHistory
      )
    }
    if (fromHistory || openHistoryMetadata == null) {
      jmmInstallerController.openRender()
      return@let
    }
    val installManifest = openHistoryMetadata.metadata
    val baseURI = when (installManifest.baseURI?.isWebUrl()) {
      true -> installManifest.baseURI!!
      else -> when (val baseUri = installManifest.baseURI) {
        null -> originUrl
        else -> buildUrlString(originUrl) { resolvePath(baseUri) }
      }.also { uri ->
        installManifest.baseURI = uri
      }
    }
    // 如果bundle_url没有host
    if (!installManifest.bundle_url.isWebUrl()) {
      installManifest.bundle_url =
        baseURI.replace("metadata.json", installManifest.bundle_url.substring(2))
    }
    debugJMM("openInstallerView", installManifest.bundle_url)
    // 这边只是打开安装界面，
    // 如果历史没有该应用，那么会直接添加，
    // 如果历史已经存在该应用，判断版本如果高于历史，替换历史版本
    val historyMetadata: JmmHistoryMetadata
    val save: Boolean
    val metadata = historyMetadataMaps[installManifest.id]
    if (metadata != null) {
      val installMM = jmmNMM.bootstrapContext.dns.query(installManifest.id)
      debugJMM("openInstallerView", "installMM=${installMM?.version}")
      if (installMM != null) { // 表示已安装过当前应用
        if (installManifest.version.isGreaterThan(installMM.version)) { // 比安装高，直接进行替换吧，暂时不考虑是否比列表中的版本高。
          oldVersion = metadata.metadata.version
          val session = getAppSessionInfo(installManifest.id, metadata.metadata.version)
          debugJMM("openInstallerView", "is order app and session=$session")
          // 如果列表的应用是下载中的，那么需要移除掉
          if (metadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
            metadata.taskId?.let { taskId -> jmmNMM.cancelDownload(taskId) }
          }
          historyMetadata = installManifest.createJmmHistoryMetadata(
            originUrl, JmmStatus.NewVersion, session?.installTime ?: datetimeNow()
          )
          save = true
        } else if (installManifest.version == installMM.version) {
          historyMetadata = installManifest.createJmmHistoryMetadata(originUrl, JmmStatus.INSTALLED)
          save = false
        } else { // 比安装的应用版本还低的，直接不能安装，提示版本过低，不存储
          historyMetadata =
            installManifest.createJmmHistoryMetadata(originUrl, JmmStatus.VersionLow)
          save = false
        }
      } else {// 从未安装过，直接替换成当前的，不考虑是否比历史列表高
        historyMetadata = openHistoryMetadata
        save = true
      }
    } else { // 从未安装过，直接替换成当前的，不考虑是否比历史列表高
      historyMetadata = openHistoryMetadata
      save = true
    }

    debugJMM("openInstallerView", historyMetadata)
    if (save) { // 只有需要存储的时候才存起来
      historyMetadataMaps[historyMetadata.metadata.id] = historyMetadata
      jmmStore.saveHistoryMetadata(
        historyMetadata.metadata.id,
        historyMetadata
      ) // 不管是否替换的，都进行一次存储新状态
    }
    jmmInstallerController.installMetadata = historyMetadata
    jmmInstallerController.openRender()
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

  private suspend fun watchProcess(metadata: JmmHistoryMetadata) {
    val taskId = metadata.taskId ?: return
    metadata.alreadyWatch = true
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.createChannelOfDownload(taskId) {
        metadata.updateByDownloadTask(downloadTask, jmmStore)
        when (downloadTask.status.state) {
          DownloadState.Completed -> {
            if (decompress(downloadTask, metadata)) {
              jmmNMM.bootstrapContext.dns.uninstall(metadata.metadata.id)
              jmmNMM.bootstrapContext.dns.install(JsMicroModule(metadata.metadata))
              metadata.installComplete(jmmStore)
            } else {
              jmmNMM.showToast(BrowserI18nResource.toast_message_download_unzip_fail.text)
              metadata.installFail(jmmStore)
            }
            // 关闭watchProcess
            channel.close()
            metadata.alreadyWatch = false
            // 删除缓存的zip文件
            jmmNMM.removeFile(downloadTask.filepath)
            // 更新完需要删除旧的app版本，这里如果有保存用户数据需要一起移动过去，但是现在这里是单纯的删除
            if (oldVersion != null) {
              jmmNMM.removeFile("/data/apps/${metadata.metadata.id}-${oldVersion}")
              oldVersion = null
            }
          }

          else -> {}
        }
      }
      debugJMM("watchProcess", "/watch process error=>$res")
    }
  }

  /**
   * 创建任务并下载，可以判断 taskId 在 download 中是否存在，如果不存在就创建，存在直接下载
   */
  suspend fun createDownloadTask(metadata: JmmHistoryMetadata) {
    val exists = metadata.taskId?.let { jmmNMM.existsDownload(it) } ?: false
    debugJMM("JmmController", "createAndStartDownloadTask exists=$exists => $metadata")
    if (!exists) {
      val taskId = with(metadata.metadata) {
        jmmNMM.createDownloadTask(url = bundle_url, total = bundle_size)
      }
      metadata.taskId = taskId
      watchProcess(metadata)
      jmmStore.saveHistoryMetadata(metadata.metadata.id, metadata)
    }
    if (metadata.state.state == JmmStatus.Downloading) {
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_downloading.text)
    }
  }

  suspend fun startDownloadTask(metadata: JmmHistoryMetadata) = metadata.taskId?.let { taskId ->
    if (!metadata.alreadyWatch) {
      watchProcess(metadata)
    }
    jmmNMM.startDownload(taskId).falseAlso {
      jmmNMM.showToast(BrowserI18nResource.toast_message_download_download_fail.text)
    }
  } ?: false

  suspend fun pause(metadata: JmmHistoryMetadata) = metadata.taskId?.let { taskId ->
    jmmNMM.pauseDownload(taskId)
  } ?: false

  private suspend fun decompress(
    task: DownloadTask,
    jmmHistoryMetadata: JmmHistoryMetadata
  ): Boolean {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    val sourcePath = jmmNMM.pickFile(task.filepath)
    val targetPath = jmmNMM.pickFile("/data/apps/$jmm")

    // 用于校验jmmApp下载文件是不是完整
    if (!jmmAppHashVerify(jmmNMM, jmmHistoryMetadata, sourcePath)) {
      return false
    }

    return jmmNMM.nativeFetch(buildUrlString("file://zip.browser.dweb/decompress") {
      parameters.append("sourcePath", sourcePath)
      parameters.append("targetPath", targetPath)
    }).boolean().trueAlso {
      // 保存 session（记录安装时间） 和 metadata （app数据源）
      jmmNMM.writeFile(
        path = "/data/apps/$jmm/usr/sys/metadata.json",
        body = IPureBody.from(Json.encodeToString(jmmHistoryMetadata.metadata))
      )
      jmmNMM.writeFile(
        path = "/data/apps/$jmm/usr/sys/session.json",
        body = IPureBody.from(Json.encodeToString(buildJsonObject {
          put("installTime", JsonPrimitive(jmmHistoryMetadata.installTime))
          put("updateTime", JsonPrimitive(datetimeNow()))
          put("installUrl", JsonPrimitive(jmmHistoryMetadata.originUrl))
        }))
      )
    }
  }

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


  suspend fun removeHistoryMetadata(historyMetadata: JmmHistoryMetadata) {
    historyMetadataMaps.remove(historyMetadata.metadata.id)
    historyMetadata.taskId?.let { taskId -> jmmNMM.removeDownload(taskId) }
    jmmStore.deleteHistoryMetadata(historyMetadata.metadata.id)
  }
}

@Serializable
data class SessionInfo(val installTime: Long, val updateTime: Long, val installUrl: String)

package org.dweb_browser.browser.jmm

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
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.download.ext.cancelDownload
import org.dweb_browser.browser.download.ext.createChannelOfDownload
import org.dweb_browser.browser.download.ext.createDownloadTask
import org.dweb_browser.browser.download.ext.currentDownload
import org.dweb_browser.browser.download.ext.existsDownload
import org.dweb_browser.browser.download.ext.pauseDownload
import org.dweb_browser.browser.download.ext.startDownload
import org.dweb_browser.browser.download.model.ChangeableMutableMap
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.debounce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.valueIn
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.sys.toast.ext.showToast

class JmmController(private val jmmNMM: JmmNMM, val jmmStore: JmmStore) {
  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  // 构建jmm历史记录
  val historyMetadataMaps: ChangeableMutableMap<String, JmmHistoryMetadata> = ChangeableMutableMap()

  // 构建历史的controller
  private val historyController = JmmHistoryController(jmmNMM, this)

  // 打开历史界面
  suspend fun openHistoryView() = historyController.openHistoryView()

  suspend fun loadHistoryMetadataUrl() {
    historyMetadataMaps.putAll(jmmStore.getHistoryMetadata())
    historyMetadataMaps.forEach { key, historyMetadata ->
      if (historyMetadata.state.state.valueIn(JmmStatus.Downloading, JmmStatus.Paused)) {
        val current = historyMetadata.taskId?.let { jmmNMM.currentDownload(it) }
          ?: -1L // 获取下载的进度，如果进度 >= 0 表示有下载
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
    originUrl: String,
    installManifest: JmmAppInstallManifest? = null,
    fromHistory: Boolean = false
  ) = installViews.getOrPut(originUrl) {
    LateInit()
  }.let {
    val jmmInstallerController = it.getOrInit {
      JmmInstallerController(
        jmmNMM,
        (installManifest ?: JmmAppInstallManifest()).createJmmHistoryMetadata(originUrl),
        this@JmmController,
        fromHistory
      )
    }
    if (installManifest != null) {
      val baseURI = when (installManifest.baseURI?.isUrlOrHost()) {
        true -> installManifest.baseURI!!
        else -> when (val baseUri = installManifest.baseURI) {
          null -> originUrl
          else -> buildUrlString(originUrl) {
            resolvePath(baseUri)
          }
        }.also { uri ->
          installManifest.baseURI = uri
        }
      }
      // 如果bundle_url没有host
      if (!installManifest.bundle_url.isUrlOrHost()) {
        installManifest.bundle_url =
          baseURI.replace("metadata.json", installManifest.bundle_url.substring(2))
      }
      debugJMM("openInstallerView", installManifest.bundle_url)
      // 存储下载过的MetadataUrl, 并更新列表，已存在，
      val historyMetadata = historyMetadataMaps.replaceOrPut(
        key = originUrl,
        replace = { jmmHistoryMetadata ->
          if (jmmNMM.bootstrapContext.dns.query(jmmHistoryMetadata.metadata.id) == null) {
            // 如果install app没有数据，那么判断当前的状态是否是下载或者暂停，如果不是这两个状态，直接当做新应用
            if (fromHistory || jmmHistoryMetadata.state.state == JmmStatus.Downloading ||
              jmmHistoryMetadata.state.state == JmmStatus.Paused
            ) {
              null // 不替换，包括来自历史
            } else {
              installManifest.createJmmHistoryMetadata(originUrl)
            }
          } else if (installManifest.version.isGreaterThan(jmmHistoryMetadata.metadata.version)) { // 如果应用中有的话，那么就判断版本是否有新版本，有点话，修改状态为 NewVersion
            oldVersion = jmmHistoryMetadata.metadata.version
            val session = getAppSessionInfo(installManifest.id, jmmHistoryMetadata.metadata.version)
            JmmHistoryMetadata(
              originUrl = originUrl,
              _metadata = installManifest,
              _state = JmmStatusEvent(
                total = installManifest.bundle_size,
                state = JmmStatus.NewVersion
              ),
              installTime = session.installTime // 使用旧的安装时间
            )
          } else null // 上面的条件都不满足的话，直接不替换
        },
        defaultValue = {
          installManifest.createJmmHistoryMetadata(originUrl)
        }
      )
      jmmStore.saveHistoryMetadata(historyMetadata.originUrl, historyMetadata) // 不管是否替换的，都进行一次存储新状态
      jmmInstallerController.installMetadata = historyMetadata
    }
    jmmInstallerController.openRender()
  }

  suspend fun uninstall(mmid: MMID): Boolean {
    val data = jmmStore.getApp(mmid) ?: return false
    // 在dns中移除app
    jmmNMM.bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除整个app
    remove("/data/apps/${mmid}-${data.installManifest.version}")
    // 从磁盘中移除整个
    jmmStore.deleteApp(mmid)
    val list = historyMetadataMaps.cMaps.values.filter { it.metadata.id == mmid }
      .sortedByDescending { it.installTime }.toMutableList()
    list.forEachIndexed { index, jmmHistoryMetadata ->
      if (index == 0) {
        jmmHistoryMetadata.updateState(JmmStatus.Init, jmmStore)
      } else {
        historyMetadataMaps.remove(jmmHistoryMetadata.originUrl)
      }
    }
    return true
  }

  suspend fun remove(filepath: String): Boolean {
    return jmmNMM.nativeFetch(
      PureClientRequest(
        "file://file.std.dweb/remove?path=${filepath}&recursive=true", PureMethod.DELETE
      )
    ).boolean()
  }

  private suspend fun watchProcess(metadata: JmmHistoryMetadata) {
    val taskId = metadata.taskId ?: return
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.createChannelOfDownload(taskId) { pureFrame, close ->
        when (pureFrame) {
          is PureTextFrame -> {
            Json.decodeFromString<DownloadTask>(pureFrame.data).also { downloadTask ->
              when (downloadTask.status.state) {
                DownloadState.Completed -> {
                  metadata.updateByDownloadTask(downloadTask, jmmStore)
                  if (decompress(downloadTask, metadata)) {
                    jmmNMM.bootstrapContext.dns.uninstall(metadata.metadata.id)
                    jmmNMM.bootstrapContext.dns.install(JsMicroModule(metadata.metadata))
                    metadata.installComplete(jmmStore)
                  } else {
                    showToastText(BrowserI18nResource.toast_message_download_unzip_fail.text)
                    metadata.installFail(jmmStore)
                  }
                  // 关闭watchProcess
                  close()
                  metadata.pauseFlag = false
                  // 删除缓存的zip文件
                  remove(downloadTask.filepath)
                  // 更新完需要删除旧的app版本，这里如果有保存用户数据需要一起移动过去，但是现在这里是单纯的删除
                  if (oldVersion != null) {
                    remove("/data/apps/${metadata.metadata.id}-${oldVersion}")
                    oldVersion = null
                  }
                }

                else -> {
                  metadata.updateByDownloadTask(downloadTask, jmmStore)
                }
              }
            }
          }

          else -> {}
        }
      }
      debugJMM("/watch process error=>", res)
    }
  }

  /**
   * 创建任务并下载，可以判断 taskId 在 download 中是否存在，如果不存在就创建，存在直接下载
   */
  suspend fun createAndStartDownloadTask(metadata: JmmHistoryMetadata) = debounce(
    scope = ioAsyncScope,
    action = {
      val exists = metadata.taskId?.let { jmmNMM.existsDownload(it) } ?: false
      debugJMM("JmmController", "createAndStartDownloadTask exists=$exists => $metadata")
      if (!exists) {
        val taskId = with(metadata.metadata) { jmmNMM.createDownloadTask(bundle_url, bundle_size) }
        metadata.taskId = taskId
        jmmStore.saveHistoryMetadata(metadata.originUrl, metadata)
      }
      if (metadata.state.state == JmmStatus.Downloading) {
        showToastText(BrowserI18nResource.toast_message_download_downloading.text)
      } else {
        startDownloadTask(metadata)
      }
    }
  )

  suspend fun startDownloadTask(metadata: JmmHistoryMetadata) = metadata.taskId?.let { taskId ->
    if (jmmNMM.startDownload(taskId)) {
      metadata.updateState(JmmStatus.Downloading, jmmStore)
      if (!metadata.pauseFlag) {
        metadata.pauseFlag = true
        watchProcess(metadata)
      }
    } else {
      showToastText(BrowserI18nResource.toast_message_download_download_fail.text)
      metadata.updateState(JmmStatus.Failed, jmmStore)
    }
  } ?: false

  suspend fun pause(taskId: TaskId?) = taskId?.let { jmmNMM.pauseDownload(taskId) } ?: false

  suspend fun cancel(taskId: TaskId?) = taskId?.let { jmmNMM.cancelDownload(taskId) } ?: false

  suspend fun exists(taskId: TaskId?) = taskId?.let { jmmNMM.existsDownload(taskId) } ?: false

  private suspend fun decompress(
    task: DownloadTask,
    jmmHistoryMetadata: JmmHistoryMetadata
  ): Boolean {
    var jmm = task.url.substring(task.url.lastIndexOf("/") + 1)
    jmm = jmm.substring(0, jmm.lastIndexOf("."))
    val sourcePath = jmmNMM.nativeFetch(buildUrlString("file://file.std.dweb/picker") {
      parameters.append("path", task.filepath)
    }).text()
    val targetPath = jmmNMM.nativeFetch(buildUrlString("file://file.std.dweb/picker") {
      parameters.append("path", "/data/apps/$jmm")
    }).text()
    return jmmNMM.nativeFetch(buildUrlString("file://zip.browser.dweb/decompress") {
      parameters.append("sourcePath", sourcePath)
      parameters.append("targetPath", targetPath)
    }).boolean().trueAlso {
      // 保存 session（记录安装时间） 和 metadata （app数据源）
      jmmNMM.nativeFetch(PureClientRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/metadata.json")
        parameters.append("create", "true")
      }, PureMethod.POST, body = IPureBody.from(Json.encodeToString(jmmHistoryMetadata.metadata))))
      jmmNMM.nativeFetch(PureClientRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/session.json")
        parameters.append("create", "true")
      }, PureMethod.POST, body = IPureBody.from(Json.encodeToString(buildJsonObject {
        put("installTime", JsonPrimitive(jmmHistoryMetadata.installTime))
        put("updateTime", JsonPrimitive(datetimeNow()))
        put("installUrl", JsonPrimitive(jmmHistoryMetadata.originUrl))
      }))))
    }
  }

  private suspend fun showToastText(message: String) = jmmNMM.showToast(message)

  private suspend fun getAppSessionInfo(mmid: MMID, version: String) =
    Json.decodeFromString<SessionInfo>(
      jmmNMM.nativeFetch("file://file.std.dweb/read?path=/data/apps/${mmid}-${version}/usr/sys/session.json")
        .text()
    )

  suspend fun removeHistoryMetadata(originUrl: String) {
    jmmStore.deleteHistoryMetadata(originUrl)
  }
}

@Serializable
data class SessionInfo(val installTime: Long, val updateTime: Long, val installUrl: String)

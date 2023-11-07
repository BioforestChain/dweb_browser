package org.dweb_browser.browser.jmm

import io.ktor.http.URLBuilder
import io.ktor.utils.io.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.jmm.model.JmmStatus
import org.dweb_browser.browser.util.isUrl
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureString
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso

class JmmController(private val jmmNMM: JmmNMM, private val store: JmmStore) {

  // 用户保存JmmInstall的数据
  private val controllerMap = mutableMapOf<MMID, JmmInstallerController>()

  // 构建历史的controller
  private val historyController = JmmHistoryController(jmmNMM, store, this)

  suspend fun loadHistoryMetadataUrl() = historyController.loadHistoryMetadataUrl()

  suspend fun openHistoryView() = historyController.openHistoryView()

  /**
   * 打开安装器视图
   */
  suspend fun openInstallerView(
    jmmAppInstallManifest: JmmAppInstallManifest, originUrl: String
  ) {
    val baseURI = when (jmmAppInstallManifest.baseURI?.isUrl()) {
      true -> jmmAppInstallManifest.baseURI!!
      else -> when (val baseUri = jmmAppInstallManifest.baseURI) {
        null -> originUrl
        else -> URLBuilder(originUrl).run {
          resolvePath(baseUri)
          buildString()
        }
      }.also {
        jmmAppInstallManifest.baseURI = it
      }
    }
    if (!jmmAppInstallManifest.bundle_url.isUrl()) {
      jmmAppInstallManifest.bundle_url = URLBuilder(baseURI).run {
        resolvePath(jmmAppInstallManifest.bundle_url)
        buildString()
      }
    }
    debugJMM("openInstallerView", jmmAppInstallManifest.bundle_url)
    // 存储下载过的MetadataUrl, 并更新列表，已存在，
    val pair = historyController.jmmHistoryMetadata.find { it.originUrl == originUrl }?.let {
      Pair(
        it.newVersion(store, jmmAppInstallManifest),
        jmmAppInstallManifest.version.isGreaterThan(it.metadata.version)
      )
    } ?: Pair(historyController.createNewMetadata(originUrl, jmmAppInstallManifest), false)

    JmmInstallerController(jmmNMM, pair.first, store, this).openRender(
      pair.second
    )


    /*val controller = controllerMap.getOrPut(jmmAppInstallManifest.id) {
      JmmInstallerController(
        jmmNMM, originUrl, jmmAppInstallManifest, historyController.getTaskId(originUrl), this
      ).also { controller ->
        controller.onJmmStateListener { pair ->
          when (pair.first) {
            JmmStatus.Init -> {
              //downloadTaskIdMap[jmmAppInstallManifest.id] = pair.second
              //store.saveJMMTaskId(jmmAppInstallManifest.id, pair.second)
            }

            JmmStatus.Completed -> {
              // 安装完成后，删除该数据的下载记录，避免状态出现问题
              //downloadTaskIdMap.remove(jmmAppInstallManifest.id)
              //store.deleteJMMTaskId(jmmAppInstallManifest.id)
            }

            JmmStatus.INSTALLED -> {
              // 安装完成，卸载之前的，安装新的
              //jmmNMM.bootstrapContext.dns.uninstall(jmmAppInstallManifest.id)
              //jmmNMM.bootstrapContext.dns.install(JsMicroModule(jmmAppInstallManifest))
              // 存储app信息到内存
              //store.setApp(
              //  jmmAppInstallManifest.id, JsMicroModuleDBItem(jmmAppInstallManifest, originUrl)
              //)
            }

            else -> {}
          }
        }
      }
    }
    controller.openRender(
      jmmAppInstallManifest.version.isGreaterThan(controller.jmmAppInstallManifest.version)
    )*/
  }

  suspend fun uninstall(mmid: MMID, version: String) {
    // 在dns中移除app
    jmmNMM.bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除整个app
    remove("/data/apps/${mmid}-${version}")
  }

  suspend fun remove(filepath: String): Boolean {
    return jmmNMM.nativeFetch(
      PureRequest(
        "file://file.std.dweb/remove?path=${filepath}&recursive=true", IpcMethod.DELETE
      )
    ).boolean()
  }

  suspend fun removeTask(taskId: TaskId): Boolean {
    return jmmNMM.nativeFetch(
      PureRequest(
        "file://download.browser.dweb/remove?taskId=${taskId}", IpcMethod.DELETE
      )
    ).boolean()
  }

  /**
   * 创建任务，如果存在则恢复
   */
  suspend fun createDownloadTask(metadataUrl: String, total: Long): PureString {
    val fetchUrl = "file://download.browser.dweb/create?url=$metadataUrl&total=$total"
    val response = jmmNMM.nativeFetch(fetchUrl)
    return response.text()
  }

  suspend fun watchProcess(
    jmmHistoryMetadata: JmmHistoryMetadata,
    callback: suspend (JmmStatus, Long, Long) -> Unit
  ) {
    val taskId = jmmHistoryMetadata.taskId ?: return
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      val readChannel = res.stream().getReader("jmm watchProcess")
      readChannel.consumeEachJsonLine<DownloadTask> { downloadTask ->
        when (downloadTask.status.state) {
          DownloadState.Init -> {
            callback(JmmStatus.Init, 0L, downloadTask.status.total)
          }

          DownloadState.Downloading -> {
            callback(JmmStatus.Downloading, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Paused -> {
            callback(JmmStatus.Paused, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Canceled -> {
            callback(JmmStatus.Canceled, downloadTask.status.current, downloadTask.status.total)
          }

          DownloadState.Failed -> {
            callback(JmmStatus.Failed, 0L, downloadTask.status.total)
          }

          DownloadState.Completed -> {
            callback(JmmStatus.Completed, downloadTask.status.current, downloadTask.status.total)
            if (decompress(downloadTask, jmmHistoryMetadata)) {
              callback(JmmStatus.INSTALLED, downloadTask.status.current, downloadTask.status.total)
            } else {
              callback(JmmStatus.Failed, 0L, downloadTask.status.total)
            }
            // 关闭watchProcess
            readChannel.cancel()
            // 删除缓存的zip文件
            remove(downloadTask.filepath)
          }
        }
      }
    }
  }

  suspend fun start(taskId: TaskId?) = taskId?.let {
    jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$taskId").boolean()
  } ?: false

  suspend fun pause(taskId: TaskId?) = taskId?.let {
    jmmNMM.nativeFetch("file://download.browser.dweb/pause?taskId=$taskId").boolean()
  } ?: false

  suspend fun cancel(taskId: TaskId?) = taskId?.let {
    jmmNMM.nativeFetch("file://download.browser.dweb/cancel?taskId=$taskId").boolean()
  } ?: false

  suspend fun exists(taskId: TaskId?) = taskId?.let {
    jmmNMM.nativeFetch("file://download.browser.dweb/exists?taskId=$taskId").boolean()
  } ?: false

  suspend fun decompress(task: DownloadTask, jmmHistoryMetadata: JmmHistoryMetadata): Boolean {
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
      jmmNMM.nativeFetch(PureRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/metadata.json")
        parameters.append("create", "true")
      }, IpcMethod.POST, body = IPureBody.from(Json.encodeToString(jmmHistoryMetadata.metadata))))
      jmmNMM.nativeFetch(PureRequest(buildUrlString("file://file.std.dweb/write") {
        parameters.append("path", "/data/apps/$jmm/usr/sys/session.json")
        parameters.append("create", "true")
      }, IpcMethod.POST, body = IPureBody.from(Json.encodeToString(buildJsonObject {
        put("installTime", JsonPrimitive(datetimeNow()))
        put("installUrl", JsonPrimitive(jmmHistoryMetadata.originUrl))
      }))))
    }
  }
}
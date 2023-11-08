package org.dweb_browser.browser.jmm

import io.ktor.http.URLBuilder
import io.ktor.utils.io.cancel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.download.model.ChangeableMutableMap
import org.dweb_browser.browser.util.isUrl
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.IPureBody
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso

class JmmController(private val jmmNMM: JmmNMM, private val store: JmmStore) {
  val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
  // 构建jmm历史记录
  val historyMetadataMaps: ChangeableMutableMap<String, JmmHistoryMetadata> = ChangeableMutableMap()
  // 构建历史的controller
  private val historyController = JmmHistoryController(jmmNMM, this)

  // 打开历史界面
  suspend fun openHistoryView() = historyController.openHistoryView()

  suspend fun loadHistoryMetadataUrl() {
    historyMetadataMaps.putAll(store.getHistoryMetadata())
    historyMetadataMaps.forEach { _, historyMetadata ->
      if (historyMetadata.state.state == JmmStatus.Downloading) {
        historyMetadata.state.state = JmmStatus.Paused
      }
    }
  }

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
    val historyMetadata = historyMetadataMaps.replaceOrPut(
      key = originUrl,
      replace = {
        if (jmmNMM.bootstrapContext.dns.query(it.metadata.id) == null) {
          // 如果install app没有数据，那么判断当前的状态是否是下载或者暂停，如果不是这两个状态，直接当做新应用
          if (it.state.state == JmmStatus.Downloading || it.state.state == JmmStatus.Paused) {
            null // 不替换
          } else {
            jmmAppInstallManifest.createJmmHistoryMetadata(originUrl)
          }
        } else if (jmmAppInstallManifest.version.isGreaterThan(it.metadata.version)) {
          // 如果应用中有的话，那么就判断版本是否有新版本，有点话，修改状态为 NewVersion
          JmmHistoryMetadata(
            originUrl = originUrl,
            metadata = jmmAppInstallManifest,
            state = JmmStatusEvent(
              total = jmmAppInstallManifest.bundle_size,
              state = JmmStatus.NewVersion
            )
          )
        } else null // 上面的条件都不满足的话，直接不替换
      },
      defaultValue = {
        jmmAppInstallManifest.createJmmHistoryMetadata(originUrl)
      }
    )
    store.saveHistoryMetadata(originUrl, historyMetadata)
    JmmInstallerController(jmmNMM, historyMetadata, this).openRender()
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
  suspend fun createDownloadTask(metadata: JmmHistoryMetadata) {
    val fetchUrl =
      "file://download.browser.dweb/create?url=${metadata.metadata.bundle_url}&total=${metadata.metadata.bundle_size}"
    val taskId = jmmNMM.nativeFetch(fetchUrl).text()
    metadata.taskId = taskId
    store.saveHistoryMetadata(metadata.originUrl, metadata)
  }

  private suspend fun watchProcess(jmmHistoryMetadata: JmmHistoryMetadata) {
    val taskId = jmmHistoryMetadata.taskId ?: return
    jmmNMM.ioAsyncScope.launch {
      val res = jmmNMM.nativeFetch("file://download.browser.dweb/watch/progress?taskId=$taskId")
      val readChannel = try {
        res.stream().getReader("jmm watchProcess")
      } catch (e: Exception) {
        return@launch
      }
      readChannel.consumeEachJsonLine<DownloadTask> { downloadTask ->
        when (downloadTask.status.state) {
          DownloadState.Completed -> {
            jmmHistoryMetadata.updateState(downloadTask, store)
            if (decompress(downloadTask, jmmHistoryMetadata)) {
              jmmNMM.bootstrapContext.dns.uninstall(jmmHistoryMetadata.metadata.id)
              jmmNMM.bootstrapContext.dns.install(JsMicroModule(jmmHistoryMetadata.metadata))
              jmmHistoryMetadata.installComplete(store)
            } else {
              jmmHistoryMetadata.installFail(store)
            }
            // 关闭watchProcess
            readChannel.cancel()
            // 删除缓存的zip文件
            remove(downloadTask.filepath)
          }

          else -> {
            jmmHistoryMetadata.updateState(downloadTask, store)
          }
        }
      }
    }
  }

  suspend fun start(jmmHistoryMetadata: JmmHistoryMetadata): Boolean {
    return jmmHistoryMetadata.taskId?.let {
      watchProcess(jmmHistoryMetadata)
      jmmNMM.nativeFetch("file://download.browser.dweb/start?taskId=$it").boolean()
    } ?: false
  }

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
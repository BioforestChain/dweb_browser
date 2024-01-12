package org.dweb_browser.browser.jmm

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadState
import org.dweb_browser.browser.download.DownloadTask
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.browser.download.model.ChangeableMutableMap
import org.dweb_browser.browser.util.isUrlOrHost
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.sys.toast.ext.showToast

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
    historyMetadataMaps.forEach { key, historyMetadata ->
      if (historyMetadata.state.state == JmmStatus.Downloading ||
        historyMetadata.state.state == JmmStatus.Paused
      ) {
        historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Paused)
      } else if (jmmNMM.bootstrapContext.dns.query(historyMetadata.metadata.id) == null) {
        historyMetadata.state = historyMetadata.state.copy(state = JmmStatus.Init) // 如果没有找到，说明被卸载了
      }
    }
  }

  private val installViews = SafeHashMap<String, LateInit<JmmInstallerController>>()

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
        }.also {
          installManifest.baseURI = it
        }
      }
      // 如果bundle_url没有host
      if (!installManifest.bundle_url.isUrlOrHost()) {
        installManifest.bundle_url = buildUrlString(baseURI) {
          this.resolvePath(installManifest.bundle_url)
        }
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
          } else if (installManifest.version.isGreaterThan(jmmHistoryMetadata.metadata.version)) {
            // 如果应用中有的话，那么就判断版本是否有新版本，有点话，修改状态为 NewVersion
            JmmHistoryMetadata(
              originUrl = originUrl,
              _metadata = installManifest,
              _state = JmmStatusEvent(
                total = installManifest.bundle_size,
                state = JmmStatus.NewVersion
              )
            )
          } else null // 上面的条件都不满足的话，直接不替换
        },
        defaultValue = {
          installManifest.createJmmHistoryMetadata(originUrl)
        }
      )
      store.saveHistoryMetadata(originUrl, historyMetadata) // 不管是否替换的，都进行一次存储新状态
      jmmInstallerController.jmmHistoryMetadata = historyMetadata
    }
    jmmInstallerController.openRender()
  }

  suspend fun uninstall(mmid: MMID): Boolean {
    val data = store.getApp(mmid) ?: return false
    val (bundleUrl, version) = data.installManifest.let { Pair(it.bundle_url, it.version) }
    debugJMM("uninstall", "$mmid-$bundleUrl $version")
    // 在dns中移除app
    jmmNMM.bootstrapContext.dns.uninstall(mmid)
    // 在存储中移除整个app
    remove("/data/apps/${mmid}-$version")
    // 从磁盘中移除整个
    store.deleteApp(mmid)
    historyMetadataMaps.cMaps.values.find { it.metadata.id == mmid }?.let { historyMetadata ->
      debugJMM("uninstall", "change history state -> $historyMetadata")
      historyMetadata.updateState(JmmStatus.Init, store)
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

  suspend fun removeTask(taskId: TaskId): Boolean {
    return jmmNMM.nativeFetch(
      PureClientRequest(
        "file://download.browser.dweb/remove?taskId=${taskId}", PureMethod.DELETE
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
      val res = jmmNMM.createChannel("file://download.browser.dweb/watch/progress?taskId=$taskId")
      { pureFrame, close ->
        when (pureFrame) {
          is PureTextFrame -> {
            Json.decodeFromString<DownloadTask>(pureFrame.data).also { downloadTask ->
              when (downloadTask.status.state) {
                DownloadState.Completed -> {
                  jmmHistoryMetadata.updateByDownloadTask(downloadTask, store)
                  if (decompress(downloadTask, jmmHistoryMetadata)) {
                    jmmNMM.bootstrapContext.dns.uninstall(jmmHistoryMetadata.metadata.id)
                    jmmNMM.bootstrapContext.dns.install(JsMicroModule(jmmHistoryMetadata.metadata))
                    jmmHistoryMetadata.installComplete(store)
                  } else {
                    showToastText(BrowserI18nResource.toast_message_download_unzip_fail.text)
                    jmmHistoryMetadata.installFail(store)
                  }
                  // 关闭watchProcess
                  close()
                  // 删除缓存的zip文件
                  remove(downloadTask.filepath)
                }

                else -> {
                  jmmHistoryMetadata.updateByDownloadTask(downloadTask, store)
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
        put("installUrl", JsonPrimitive(jmmHistoryMetadata.originUrl))
      }))))
    }
  }

  suspend fun showToastText(message: String) = jmmNMM.showToast(message)
}



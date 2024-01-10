package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod

val debugFileChooser = Debugger("FileChooser")

/**
 * https://github.com/BioforestChain/dweb_browser/issues/58
 */
class FileChooserNMM : NativeMicroModule("fs-picker.sys.dweb", "FileChooser") {
  private val fileChooserManage = FileChooserManage()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/open-file" bind PureMethod.GET by defineJsonResponse {
        // /open-file?accept=*&multiple=*&limit=* 选择文件
        // accept 表示类型， multiple表示是否开启多选， limit表示最多可以选几个
        // accept 的格式为mime:ext,ext;mime:ext，比如：image/*:.jpg,.png;video/*:.mp4
        val accept = request.queryOrNull("accept") ?: "*/*"
        val multiple = request.queryOrNull("multiple")?.toBoolean() ?: false
        val limit = request.queryOrNull("limit")?.toInt() ?: 1
        debugFileChooser("open-file", "accept=$accept, multiple=$multiple, limit=$limit")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@FileChooserNMM
        fileChooserManage.openFileChooser(fromMM, accept, multiple, limit).toJsonElement()
      },
      "/directory" bind PureMethod.GET by defineEmptyResponse {
        // /directory?mode=*&startIn=*&preference=* 选择目录
        // mode 为 readonly|readwrite, startIn 为 desktop|documents|downloads|music|pictures|videos|... 取决于操作系统支持的程度，这里这是提供一个建议路径
      },
      "/save-file" bind PureMethod.GET by defineEmptyResponse {
        // /save-file?suggestedName=*&startIn=*&preference=* 保存文件
        // suggestedName 为默认的文件名称
      }
    )
  }

  override suspend fun _shutdown() {
  }
}
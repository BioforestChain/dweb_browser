package org.dweb_browser.browser.jsProcess

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.ipcWeb.DMessagePortIpc
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.SimpleCallback

@Serializable
data class ProcessInfo(val process_id: Int)

class ProcessHandler(val info: ProcessInfo, var ipc: DMessagePortIpc)

data class RunProcessMainOptions(val main_url: String)

interface IJsProcessWebApi {
  suspend fun createProcess(
    env_script_url: String,
    metadata_json: String,
    env_json: String,
    remoteModule: IMicroModuleManifest,
    host: String
  ): ProcessHandler

  suspend fun createIpcFail(
    process_id: String,
    mmid: String,
    reason: String
  )

  suspend fun runProcessMain(process_id: Int, options: RunProcessMainOptions)

  suspend fun destroyProcess(process_id: Int)

  suspend fun createIpc(process_id: Int, mmid: MMID): Int

  suspend fun destroy()

  suspend fun onDestory(cb: SimpleCallback) : OffListener<Unit>
}

expect suspend fun createJsProcessWeb(mainServer: HttpDwebServer, mm: NativeMicroModule): IJsProcessWebApi

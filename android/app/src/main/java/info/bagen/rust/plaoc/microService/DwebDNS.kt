package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.network.*
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.suspendCoroutine

typealias Domain = String;
// 声明全局dns

val global_dns = DwebDNS()

class DwebDNS : NativeMicroModule() {
    private val mmMap = mutableMapOf<Domain, MicroModule>()

    private val jsMicroModule = JsMicroModule()
    private val bootNMM = BootNMM()
    private val multiWebViewNMM = MultiWebViewNMM()
    private val httpNMM = HttpNMM()

    override fun _bootstrap() {
        install(this)
        install(bootNMM)
        install(jsMicroModule)
        install(multiWebViewNMM)
        install(httpNMM)

        /** 对等连接列表 */
        val connects = mutableMapOf<MicroModule, MutableMap<Mmid, Ipc>>()

        fetchAdaptor = { fromMM, request ->
            if (request.uri.scheme === "file:" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                mmMap[mmid]?.let {
                    /** 一个互联实例表 */
                    val ipcMap = connects.getOrPut(fromMM) { mutableMapOf() }

                    /**
                     * 一个互联实例
                     */
                    val ipc = ipcMap.getOrPut(mmid) {
                        val toMM = open(mmid);
                        toMM.connect(fromMM).also { ipc ->
                            /// 在 IPC 关闭的时候，从 ipcMap 中移除
                            ipc.onClose { ipcMap.remove(mmid) }
                        }
                    }
//                    ipc.request
//                    return@let ipc
                    return@let null
                } ?: null
            } else null
        }

        runBlocking {
            val boot = nativeFetch("file://boot.sys.dweb/open?origin=desktop.bfs.dweb")
            println("startBootNMM# boot response: $boot")
        }
    }

    override fun _shutdown() {
        fetchAdaptor = null
        mmMap.forEach {
            it.value.shutdown()
        }
        mmMap.clear()
    }

    private val running_apps = mutableMapOf<Mmid, MicroModule>();

    /** 安装应用 */
    private fun install(mm: MicroModule) {
        mmMap[mm.mmid] = mm
    }

    /** 查询应用 */
    private suspend fun query(mmid: Mmid): MicroModule? {
        return mmMap[mmid]
    }

    /** 打开应用 */
    private suspend fun open(mmid: Mmid): MicroModule {
        return running_apps.getOrPut(mmid) {
            query(mmid)?.also {
                it.bootstrap()
            } ?: throw  Exception("no found app: $mmid")
        }
    }

    /** 关闭应用 */
    private suspend fun close(mmid: Mmid): Int {
        return running_apps.remove(mmid)?.let {
            try {
                it.shutdown()
                1
            } catch (_: Exception) {
                0
            }
        } ?: -1
    }
}





package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.network.*

typealias Domain = String;
// 声明全局dns

val global_dns = DwebDNS()

class DwebDNS : NativeMicroModule() {
    private val dnsTables = mutableMapOf<Domain, MicroModule>()

    val jsMicroModule = JsMicroModule()
    private val bootNMM = BootNMM()
    val multiWebViewNMM = MultiWebViewNMM()
    val httpNMM = HttpNMM()

    init {
        dnsTables["mwebview.sys.dweb"] = multiWebViewNMM
        dnsTables["boot.sys.dweb"] = bootNMM
        dnsTables["js.sys.dweb"] = jsMicroModule
        dnsTables["localhost.sys.dweb"] = httpNMM
        dnsTables["dns.sys.dweb"] = this

        fetchAdaptor = { remote, request ->
            if (request.uri.scheme === "file:" && request.uri.host.endsWith(".dweb")) {
                val mmid = request.uri.host
                dnsTables[mmid]?.let { mm ->
                    null
                }
            }
            null
        }
    }

    fun add(mmid: Mmid, microModule: MicroModule): Boolean {
        dnsTables[mmid] = microModule
        return true
    }

    fun close(mmid: Mmid): String {
        if (dnsTables.containsKey(mmid)) {
            return dnsTables.remove(mmid)!!.mmid
        }
        return "false"
    }

    override fun _bootstrap() {
        val boot = nativeFetch("file://boot.sys.dweb/open?origin=desktop.bfs.dweb")
        println("startBootNMM# boot response: $boot")
    }

    override fun _shutdown() {
        dnsTables.forEach {
            it.value.shutdown()
        }
    }
}





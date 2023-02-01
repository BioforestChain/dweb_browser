package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity


typealias Domain = String;
typealias Mmid = String;
val dns_map = mutableMapOf<Mmid, (data: BootOptions) -> Unit>()

class DwebDNS {
    val dnsTables = mutableMapOf<Domain, MicroModule>()
    init {
        initDnsMap()
    }

    fun add(mmid:Mmid,microModule: MicroModule): Boolean {
        dnsTables[mmid] = microModule
        return true
    }
    fun remove(mmid: Mmid): String {
        if (dnsTables.containsKey(mmid)) {
            return dnsTables.remove(mmid)!!.mmid
        }
        return "false"
    }

    private fun initDnsMap() {
        dns_map["desktop.sys.dweb"] = {
            openHomeActivity()
        }
    }

}






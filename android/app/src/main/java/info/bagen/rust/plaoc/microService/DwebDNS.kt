package info.bagen.rust.plaoc.microService

typealias Domain = String;
typealias Mmid = String;

class DwebDNS {
    val dnsTables = mutableMapOf<Domain, MicroModule>()
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
}



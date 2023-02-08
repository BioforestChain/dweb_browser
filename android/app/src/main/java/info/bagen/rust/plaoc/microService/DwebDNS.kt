package info.bagen.rust.plaoc.microService

import android.net.Uri

typealias Domain = String;

// 声明全局dns
val global_micro_dns = DwebDNS()

class DwebDNS : NativeMicroModule() {
    private val dnsTables = mutableMapOf<Domain, MicroModule>()

    private val jsMicroModule = JsMicroModule()
    private val bootNMM = BootNMM()
    private val multiWebViewNMM = MultiWebViewNMM()
    private val httpNMM = HttpNMM()

    init {
        dnsTables["mwebview.sys.dweb"] = multiWebViewNMM
        dnsTables["boot.sys.dweb"] = bootNMM
        dnsTables["js.sys.dweb"] = jsMicroModule
        dnsTables["localhost.sys.dweb"] = httpNMM
        dnsTables["dns.sys.dweb"] = this
    }

    /** 转发dns到各个微组件
     *  file://
     *  */
    fun nativeFetch(url: String): Any? {
        Uri.parse(url)?.let { uri ->
            val mmid = uri.host
            println("kotlin#nativeFetch mmid==> $mmid routerTarget==> ${uri.path}")
            // 看看有没有匹配到mmid
            if (dnsTables.containsKey(mmid)) {
                // 有没有传递路由
                uri.path?.let { router ->
                    return dnsTables[mmid]?.bootstrap(router, uri.queryParameterByMap())
                }
                return "Error not found routerTarget"
            }
            return "Error not found $mmid domain"
        }
        return "Error url not parse to uri => $url"
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
}





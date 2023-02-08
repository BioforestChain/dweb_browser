package info.bagen.rust.plaoc.microService

import android.net.Uri
import java.net.URLDecoder

typealias Domain = String;
typealias Mmid = String;

// 声明全局dns
val global_micro_dns = DwebDNS()

class DwebDNS : NativeMicroModule() {
    private val dnsTables = mutableMapOf<Domain, MicroModule>()

    private val jsMicroModule = JsMicroModule()
    private val bootNMM = BootNMM()
    private val multiWebViewNMM = MultiWebViewNMM()
    private val localhostNMM = LocalhostNMM()

    init {
        dnsTables["mwebview.sys.dweb"] = multiWebViewNMM
        dnsTables["boot.sys.dweb"] = bootNMM
        dnsTables["js.sys.dweb"] = jsMicroModule
        dnsTables["localhost.sys.dweb"] = localhostNMM
        dnsTables["dns.sys.dweb"] = this
    }

    /** 转发dns到各个微组件
     *  file://
     *  */
    fun nativeFetch(url: String): Any? {
        Uri.parse(url)?.let { uri ->
            val mmid = uri.host
            println("kotlin#nativeFetch mmid==> $mmid routerTarget==> ${uri.path}")
            if (dnsTables.containsKey(mmid)) {
                uri.path?.let { path ->
                    return dnsTables[mmid]?.bootstrap(path, uri.queryParameterByMap())
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

fun Uri.queryParameterByMap(): HashMap<String, String> {
    val hashMap = hashMapOf<String, String>()
    this.queryParameterNames.forEach { name ->
        val key = URLDecoder.decode(name, "UTF-8")
        val value = URLDecoder.decode(this.getQueryParameter(name), "UTF-8")
        hashMap[key] = value
    }
    return hashMap
}






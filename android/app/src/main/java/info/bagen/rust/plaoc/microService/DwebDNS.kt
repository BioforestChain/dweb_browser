package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity
import java.net.URLDecoder


typealias Domain = String;
typealias Mmid = String;
// 声明全局dns
val global_micro_dns = DwebDNS()

class DwebDNS : NativeMicroModule() {
    private val dnsTables = mutableMapOf<Domain, MicroModule>()
    val dnsMap = mutableMapOf<Mmid, (data: NativeOptions) -> Unit>()

    private val jsMicroModule = JsMicroModule()
    private val bootNMM = BootNMM()
    private val multiWebViewNMM = MultiWebViewNMM()

    init {
        initDnsMap()
        dnsTables["mwebview.sys.dweb"] = multiWebViewNMM
        dnsTables["boot.sys.dweb"] = bootNMM
        dnsTables["js.sys.dweb"] = jsMicroModule
    }

    /** 转发dns到各个微组件
     *  file://
     *  */
    fun nativeFetch(url: String) {
        val tmp = url.substring(7)
        val mmid = tmp.substring(0, tmp.indexOf("/"))
        println("kotlin#nativeFetch mmid==> $mmid tmp==> $tmp")
        val option = fetchMatchParam(tmp)
        dnsTables.keys.forEach { domain ->
            if (mmid.contains(domain)) {
//                println("kotlin#fetchMatchParam bootOptions ==> ${option.origin},${option.mainJs} ")
                dnsTables[domain]?.bootstrap(option)
            }
        }
    }

    private val bootOptionParams = mutableSetOf("origin", "mainCode", "main_code")

    /** 截取参数 */
    private fun fetchMatchParam(url: String): NativeOptions {
        var routerTarget = ""
        var query = ""
        // 如果是这种类型的 open?xx=xxx
        if (url.indexOf("?") > 0) {
            routerTarget = url.substring(url.indexOf("/") + 1,url.indexOf("?"))
            query = url.substring(url.indexOf("?") + 1)
        } else {
            // 如果是这种类型的 /listen/(:webview_id)
            val lIdx = url.lastIndexOf("/")
            val idx = url.indexOf("/")
            if (lIdx < 0) {
                routerTarget = url.substring(idx + 1,lIdx)
                query = url.substring(lIdx)
            } else {
                // 如果是这种类型的请求 mwebview.sys.dweb/open 没有参数 那么就不需要截取最后的 / 直接取出 open(routerTarget)
                routerTarget = url.substring(idx + 1)
            }
        }
        // 处理传递的参数
        val pairs = query.split("&")
        val bootOptions = NativeOptions(routerTarget=routerTarget)
        pairs.forEach { pair ->
            val idx = pair.indexOf("=")
            //如果等号存在且不在字符串两端，取出key、value
            if (idx > 0 && idx < pair.length - 1) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
//                println("kotlin#fetchMatchParam key ==> $key value ==> $value ")
                // 取出后构建一个参数对象返回
                if (bootOptionParams.contains(key)) {
                    bootOptions[key] = value
                }
            }
        }
        return bootOptions
    }

    fun add(mmid: Mmid, microModule: MicroModule): Boolean {
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
        // 启动对应的功能
        dnsMap["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
        dnsMap["mwebview.sys.dweb"] = { webView ->
            println("kotlin#initDnsMap routerTarget==> ${webView.routerTarget} ")
            multiWebViewNMM.routers[webView.routerTarget]?.let { it -> it(webView) }
        }
    }
}






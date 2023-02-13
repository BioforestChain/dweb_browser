package info.bagen.rust.plaoc.microService

import android.net.Uri
import java.net.URLDecoder

/** 启动Boot服务*/
fun startBootNMM() {
//    val boot = global_micro_dns.nativeFetch("file://boot.sys.dweb/open?origin=desktop.bfs.dweb")
//    println("startBootNMM# boot response: $boot")
}

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
}

typealias Mmid = String;
typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>


abstract class MicroModule {
    open val mmid: String = ""
//    abstract fun bootstrap(routerTarget: String, options: NativeOptions): Any?
}

fun Uri.queryParameterByMap(): NativeOptions {
    val hashMap = hashMapOf<String, String>()
    this.queryParameterNames.forEach { name ->
        val key = URLDecoder.decode(name, "UTF-8")
        val value = URLDecoder.decode(this.getQueryParameter(name), "UTF-8")
        hashMap[key] = value
    }
    return hashMap
}
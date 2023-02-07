package info.bagen.rust.plaoc.microService


/** 启动Boot服务*/
fun startBootNMM() {
   val boot = global_micro_dns.nativeFetch("file://boot.sys.dweb/open?origin=desktop.bfs.dweb")
    println("startBootNMM# boot response: $boot")
}

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
     override fun bootstrap(args:MutableMap<String, String>):Any? {
         println("kotlin#NativeMicroModule bootstrap==> ${args["mainCode"]}  ${args["origin"]}")
         return args["origin"]
     }
}

typealias NativeOptions = MutableMap<String, String>
//open class NativeOptions(
//    var origin: String = "", // 程序地址
//    var mainCode: String = "", // webWorker运行地址
//    val routerTarget:String,
//    val processId: Int? = null,  // 要挂载的父进程id
//    val webViewId: String = "default", // default.mwebview.sys.dweb
//) {
//    operator fun set(key: String?, value: String?) {
//        if (key == "origin" && value != null) {
//            this.origin = value
//            return
//        }
//        if ((key == "main_code" || key == "mainCode" ) && value != null) {
//            this.mainCode = value
//            return
//        }
//    }
//}

abstract class MicroModule {
    open val mmid: String = ""
    abstract fun bootstrap(args:NativeOptions): Any?
}
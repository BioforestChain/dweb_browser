package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import info.bagen.rust.plaoc.microService.ipc.Ipc


typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>


abstract class MicroModule {
    open val mmid: Mmid = ""
    open val routers: Router? = null
    protected abstract suspend fun _bootstrap()

    private var runningStateLock = PromiseOut.resolve(false)
    val running get() = runningStateLock.value == true

    private suspend fun beforeBootstrap() {
        if (this.runningStateLock.waitPromise()) {
            throw Exception("module ${this.mmid} already running");
        }
        this.runningStateLock = PromiseOut()
    }

    private suspend fun afterBootstrap() {
        this.runningStateLock.resolve(true)
    }

    suspend fun bootstrap() {
        this.beforeBootstrap()

        try {
            this._bootstrap();
        } finally {
            this.afterBootstrap();
        }
    }

    protected val _afterShutdownSignal = SimpleSignal();

    protected suspend fun beforeShutdown() {
        if (!this.runningStateLock.waitPromise()) {
            throw Exception("module $mmid already shutdown");
        }
        this.runningStateLock = PromiseOut()
    }

    protected abstract suspend fun _shutdown()
    protected open suspend fun afterShutdown() {
        _afterShutdownSignal.emit()
        _afterShutdownSignal.clear()
        runningStateLock.resolve(false)
    }


    suspend fun shutdown() {
        this.beforeShutdown()

        try {
            this._shutdown()
        } finally {
            this.afterShutdown()
        }
    }

    /** 外部程序与内部程序建立链接的方法 */
    protected abstract suspend fun _connect(from: MicroModule): Ipc;
    suspend fun connect(from: MicroModule): Ipc {
        if (!runningStateLock.waitPromise()) {
            throw Exception("module no running");
        }
        return _connect(from);
    }

}

//fun Uri.queryParameterByMap(): NativeOptions {
//    val hashMap = hashMapOf<String, String>()
//    this.queryParameterNames.forEach { name ->
//        val key = URLDecoder.decode(name, "UTF-8")
//        val value = URLDecoder.decode(this.getQueryParameter(name), "UTF-8")
//        hashMap[key] = value
//    }
//    return hashMap
//}
package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import info.bagen.rust.plaoc.microService.ipc.Ipc
import kotlinx.coroutines.sync.Mutex


typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>


abstract class MicroModule {
    open val mmid: Mmid = ""
    open val routers: Router? = null
    protected abstract suspend fun _bootstrap()

    private var running = false;
    private var _bootstrapLock: Mutex? = Mutex()

    private suspend fun beforeBootstrap() {
        if (this.running) {
            throw  Exception("module ${this.mmid} already running");
        }
        this.running = true;
    }

    protected suspend fun afterBootstrap() {
//        println("MicroModule#afterBootstrap===>${this.mmid}  ")
    }

    suspend fun bootstrap() {
        this.beforeBootstrap()

        val bootstrapLock = Mutex()
        this._bootstrapLock = bootstrapLock
        try {
            this._bootstrap();
        } finally {
            if (bootstrapLock.isLocked) {
                bootstrapLock.unlock();
            }
            this._bootstrapLock = null;

            this.afterBootstrap();
        }
    }

    protected val _afterShutdownSignal = SimpleSignal();

    protected suspend fun beforeShutdown() {
        if (!running) {
            throw  Exception("module $mmid already shutdown");
        }
        running = false;
        _afterShutdownSignal.emit()
    }

    private var _shutdownLock: Mutex? = null
    protected abstract suspend fun _shutdown()
    protected open suspend fun afterShutdown() {}

    suspend fun shutdown() {
        if (this._bootstrapLock!!.isLocked) {
            this._bootstrapLock
        }

        val shutdownLock = Mutex()
        this._shutdownLock = shutdownLock
        this.beforeShutdown()
        try {
            this._shutdown()
        } finally {
            shutdownLock.unlock()
            this._shutdownLock = null
            this.afterShutdown()
        }
    }

    /** 外部程序与内部程序建立链接的方法 */
    protected abstract suspend fun _connect(from: MicroModule): Ipc;
    suspend fun connect(from: MicroModule): Ipc {
        if (!running) {
            throw Exception("module no running");
        }
        _bootstrapLock?.lock()
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
package info.bagen.rust.plaoc.microService

import android.net.Uri
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.wait
import java.net.URLDecoder


typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>


abstract class MicroModule {
    open val mmid: Mmid = ""
    open val routers: Router? = null
    protected abstract fun _bootstrap()

    private var running = false;
    private var _bootstrapLock: Mutex? = Mutex()

    private fun beforeBootstrap() {
        if (this.running) {
            throw  Error("module ${this.mmid} already running");
        }
        this.running = true;
    }

    protected fun afterBootstrap() {
    }

    fun bootstrap() {
        this.beforeBootstrap()

        val bootstrapLock = Mutex()
        this._bootstrapLock = bootstrapLock
        try {
            this._bootstrap();
        } finally {
            bootstrapLock.unlock();
            this._bootstrapLock = null;

            this.afterBootstrap();
        }
    }

    protected fun beforeShutdown() {
        if (!this.running) {
            throw  Error("module ${this.mmid} already shutdown");
        }
        this.running = false;
    }

    private var _shutdownLock: Mutex? = null
    protected abstract fun _shutdown()
    protected open fun afterShutdown() {}

    fun shutdown() {
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
        if (running === false) {
            throw Exception("module no running");
        }
        _bootstrapLock?.wait()
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
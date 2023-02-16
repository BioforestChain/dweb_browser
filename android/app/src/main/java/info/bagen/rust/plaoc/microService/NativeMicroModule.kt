package info.bagen.rust.plaoc.microService

import android.net.Uri
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.net.URLDecoder

abstract class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {

}

typealias Mmid = String;
typealias Router = MutableMap<String, AppRun>
typealias AppRun = (options: NativeOptions) -> Any
typealias NativeOptions = MutableMap<String, String>


abstract class MicroModule {
    open val mmid: String = ""
    open val routers:Router? = null
    protected abstract  fun _bootstrap()

    private var running = false;
    private var _bootstrapLock :Mutex? = Mutex()

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
    protected fun afterShutdown() {}

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

    fun connent() {

    }
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
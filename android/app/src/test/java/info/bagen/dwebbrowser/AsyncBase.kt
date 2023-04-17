package info.bagen.dwebbrowser;

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.debug.DebugProbes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

open class AsyncBase {
    companion object {
        var deferredList = mutableListOf<Deferred<Unit>>()
        val prepareReady = CompletableDeferred<Unit>().also { deferredList += it }

        @JvmStatic
        protected val catcher = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
    }

    inline suspend fun awaitAll() {
        prepareReady.await()
        for (def in deferredList) {
            def.await()
        }
    }

    @BeforeEach
    fun debugInstall() {
        DebugProbes.install()

    }

    @AfterEach
    fun debugUninstall() {
        DebugProbes.uninstall()
    }

    inline fun printDumpCoroutinesInfo() {
        var i = 1
        println("job.isCompleted: \n${
            DebugProbes.dumpCoroutinesInfo().joinToString(separator = "\n") { it ->
                "${i++}.\t| $it"
            }
        }")
    }

    inline fun enableDwebDebug(flags: List<String>) {
        System.setProperty("dweb-debug", flags.joinToString(separator = " ") { it })
    }

}

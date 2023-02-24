package info.bagen.rust.plaoc;

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.debug.DebugProbes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

open class AsyncBase {

    companion object {
        @JvmStatic
        protected val catcher = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
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

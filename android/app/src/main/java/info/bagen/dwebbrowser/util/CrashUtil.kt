package info.bagen.dwebbrowser.util

import android.content.Context
import android.os.Build
import android.os.Process
import android.widget.Toast
import com.qiniu.android.storage.UploadManager
import io.ktor.server.engine.DefaultUncaughtExceptionHandler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.Thread.UncaughtExceptionHandler
import java.lang.Thread.setDefaultUncaughtExceptionHandler


class CrashUtil : UncaughtExceptionHandler {
  private lateinit var appContext: Context
  private val UPTOKEN_Z0 =
    "vO3IeF4GypmPpjMnkHcZZo67hHERojsvLikJxzj5:s9dW6FAc8c88zMiZorpm6eudjAc=:eyJzY29wZSI6ImphY2tpZS15ZWxsb3c6Y3Jhc2giLCJkZWFkbGluZSI6MTY5ODc1NjI4NSwiaXNQcmVmaXhhbFNjb3BlIjoxfQ=="
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler // 用于全局的协程调用
  private val defaultException = Thread.getDefaultUncaughtExceptionHandler()

  companion object {
    val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      CrashUtil()
    }
  }

  fun init(context: Context) {
    appContext = context
    setDefaultUncaughtExceptionHandler(this)

    ioAsyncScope.launch(ioAsyncExceptionHandler) {
      val regex = "^crash_.*\\.log$".toRegex() // 以crash_打头，.log 结尾
      val uploadManager = UploadManager()
      appContext.cacheDir.listFiles()?.forEach { file ->
        if (file.isFile && regex.matches(file.name)) {
          upload(file, uploadManager) {}
        }
      }
    }
  }

  override fun uncaughtException(p0: Thread, p1: Throwable) {
    try {
      val fileName = "crash_${Build.MANUFACTURER}_${System.currentTimeMillis()}.log"
      val file = File("${appContext.cacheDir.absolutePath}/$fileName")
      val fos = FileOutputStream(file)
      fos.write("MANUFACTURER: ${Build.MANUFACTURER}\r\n".toByteArray())
      fos.write("MODEL: ${Build.MODEL}\r\n".toByteArray())
      fos.write("HARDWARE: ${Build.HARDWARE}\r\n".toByteArray())
      fos.write("Android Version: ${Build.VERSION.RELEASE}\r\n".toByteArray())
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
        fos.write("SOC_MANUFACTURER: ${Build.SOC_MANUFACTURER}\r\n".toByteArray())
        fos.write("SOC_MODEL: ${Build.SOC_MODEL}\r\n".toByteArray())
        fos.write("SKU: ${Build.SKU}\r\n".toByteArray())
      }
      val printStream = PrintStream(fos)
      p1.printStackTrace(printStream)
      printStream.flush()
      printStream.close()
      fos.close()
      defaultException?.uncaughtException(p0, p1)
      val uploadManager = UploadManager()
      upload(file, uploadManager) {
        Process.killProcess(Process.myPid())
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Process.killProcess(Process.myPid())
    }
  }

  private fun upload(file: File, uploadManager: UploadManager, handlerCallback: () -> Unit) {
    uploadManager.put(file, file.name, UPTOKEN_Z0, { _, respInfo, jsonData ->
      if (respInfo.isOK) {
        // println("异常上报成功")
        file.deleteRecursively()
      } else {
        println("异常上报失败")
      }
      handlerCallback()
    }, null)
  }
}
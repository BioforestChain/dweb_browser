import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.ktor.getPort
import java.io.File
import java.net.BindException

object WindowsSingleInstance {
  val singleInstanceFlow = MutableSharedFlow<String>(replay = 1)

  private suspend fun createSingleInstanceServer(port: Int = 0) =
    try {
      val server = embeddedServer(Netty, port = port) {
        install(createApplicationPlugin("dweb_deeplink") {
          onCall { call ->
            withContext(defaultAsyncExceptionHandler) {
              val deeplink = call.request.queryParameters["url"]
              singleInstanceFlow.emit(deeplink!!)
              call.respond(HttpStatusCode.OK)
            }
          }
        })
      }
      server.start(wait = false).let {
        server.engine.getPort()
      }
    } catch (e: BindException) {
      0
    } catch (e: Throwable) {
      println("尝试启动单实例服务器失败: ${e.message} ${e.stackTraceToString()}")
      throw e
    }

  suspend fun requestSingleInstance(deeplink: String = ""): Boolean {
    val root = File("${System.getProperty("user.home")}/.dweb")
    if (!root.exists()) {
      root.mkdirs()
    }
    val portFile = File("${root.absolutePath}/deeplinkPort.txt")
    if (!portFile.exists()) {
      val port = createSingleInstanceServer()
      withContext(ioAsyncExceptionHandler) {
        portFile.createNewFile()
        val portContent = "$port"
        portFile.writeText(portContent)

        // 添加应用关闭删除端口文件，减少因为其它程序占用该端口导致应用无法启动几率
        Runtime.getRuntime().addShutdownHook(Thread {
          try {
            portFile.delete()
          } catch (e: Throwable) {
            //
          }
        })
      }
    } else {
      val port = portFile.readText().toInt()
      if (createSingleInstanceServer(port) == 0) {
        if (deeplink.isNotEmpty()) {
          httpFetch("http://localhost:$port/?url=$deeplink")
        }
        return false
      }
    }

    return true
  }
}
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.permission.AuthorizationRecord
import org.dweb_browser.core.std.permission.PermissionHooks
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.permissionStdProtocol
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionTest {

  class FakePermissionNMM() : NativeMicroModule("fake-permission.test.dweb", "Faker") {
    init {
      dweb_protocols = listOf("permission.std.dweb")
    }

    inner class FakePermissionRuntime(override val bootstrapContext: BootstrapContext) :
      NativeMicroModule.NativeRuntime() {
      override suspend fun _bootstrap() {
        permissionStdProtocol(object : PermissionHooks {
          override suspend fun onRequestPermissions(
            applicantIpc: Ipc,
            permissions: List<PermissionProvider>,
          ): Map<PermissionProvider, AuthorizationRecord> {
            println(
              "QWQ onRequestPermissions applicantIpc=$applicantIpc permissions=${
                permissions.joinToString(",")
              }"
            )
            return permissions.associateWith {
              AuthorizationRecord.generateAuthorizationRecord(
                it.pid, applicantIpc.remote.mmid, true
              )
            }
          }
        })
      }

      override suspend fun _shutdown() {
      }

    }

    override fun createRuntime(bootstrapContext: BootstrapContext) =
      FakePermissionRuntime(bootstrapContext)

  }

  class ServerNMM(mmid: String = "server.test.dweb") : NativeMicroModule(mmid, "server") {
    init {
      dweb_permissions = listOf(
        DwebPermission(
          pid = "$mmid/demo",
          routes = listOf("file://$mmid/demo"),
          title = "测试权限",
        )
      )
    }

    inner class ServerRuntime(override val bootstrapContext: BootstrapContext) :
      NativeMicroModule.NativeRuntime() {
      override suspend fun _bootstrap() {
        routes("/demo" bind PureMethod.GET by defineStringResponse {
          "okk"
        })
      }

      override suspend fun _shutdown() {
      }
    }

    override fun createRuntime(bootstrapContext: BootstrapContext) = ServerRuntime(bootstrapContext)
  }

  class ClientNMM(mmid: String = "client.test.dweb") : NativeMicroModule(mmid, "client") {

    inner class ClientRuntime(override val bootstrapContext: BootstrapContext) :
      NativeMicroModule.NativeRuntime() {
      override suspend fun _bootstrap() {
      }

      override suspend fun _shutdown() {
      }
    }

    override fun createRuntime(bootstrapContext: BootstrapContext) = ClientRuntime(bootstrapContext)
  }

  @Test
  fun testPermission() = runCommonTest {
    val dns = DnsNMM()
    dns.install(FileNMM())
    dns.install(FakePermissionNMM())
    val serverNMM = ServerNMM()
    dns.install(serverNMM)
    val clientNMM = ClientNMM()
    dns.install(clientNMM)

    val dnsRuntime = dns.bootstrap()
    val clientRuntime = dnsRuntime.open(clientNMM.mmid)
    val response = clientRuntime.nativeFetch("file://${serverNMM.mmid}/demo")
    println("QWQ response=$response")
    assertEquals(response.text(), "okk")
    println("QWQ okk")
  }
}
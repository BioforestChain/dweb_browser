package org.dweb_browser.sys.keychain


import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.keychain.render.Render
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

val debugKeychain = Debugger("keychain")

class KeychainNMM : NativeMicroModule("keychain.sys.dweb", KeychainI18nResource.name.text) {

  init {
    short_name = KeychainI18nResource.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Device_Management_Service,
    )
    icons =
      listOf(ImageResource(src = "file:///sys/sys-icons/$mmid.svg", type = "image/svg+xml"))
  }

  inner class KeyChainRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    private val keyChainStore = KeychainStore(this)
    override suspend fun _bootstrap() {
      routes(
        "/get" bind PureMethod.GET by definePureBinaryHandler {
          runCatching {
            keyChainStore.getItem(ipc.remote.mmid, request.query("key"))
          }.getOrElse { throwException(HttpStatusCode.Forbidden, cause = it) } ?: throwException(
            HttpStatusCode.NotFound
          )
        },
        "/keys" bind PureMethod.GET by defineJsonResponse {
          keyChainStore.keys(ipc.remote.mmid).toJsonElement()
        },
        "/has" bind PureMethod.GET by defineBooleanResponse {
          keyChainStore.hasItem(ipc.remote.mmid, request.query("key"))
        },
        "/delete" bind PureMethod.GET by defineBooleanResponse {
          keyChainStore.deleteItem(ipc.remote.mmid, request.query("key"))
        },
        "/set" bind PureMethod.GET by defineBooleanResponse {
          keyChainStore.setItem(
            ipc.remote.mmid,
            request.query("key"),
            request.query("value").let { value ->
              when (val encoding = request.queryOrNull("encoding")?.lowercase()) {
                "base64" -> value.base64Binary
                "utf8", "utf-8", null -> value.utf8Binary
                else -> throwException(
                  HttpStatusCode.InternalServerError, "invalid value encoding:${encoding}"
                )
              }
            },
          )
        },
        "/keys" bind PureMethod.GET by defineJsonResponse {
          keyChainStore.keys(ipc.remote.mmid).toJsonElement()
        },
      ).cors()


      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
          windowAdapterManager.provideRender(id) { modifier ->
            keychainManager.Render(modifier, this)
          }
        }
      }
    }


    internal val keychainManager = KeychainManager(this, keyChainStore)

    override suspend fun _shutdown() {

    }


  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = KeyChainRuntime(bootstrapContext)
  private fun String.removeArrayMark() = this.replace("[", "").replace("]", "")
}
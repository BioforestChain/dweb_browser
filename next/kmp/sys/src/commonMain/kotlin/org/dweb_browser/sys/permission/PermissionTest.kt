package org.dweb_browser.sys.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.ext.deletePermission
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

class PermissionProviderTNN :
  NativeMicroModule("provider.test.permission.sys.dweb", "Permission Provider") {
  init {
    short_name = name
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
    )
    icons = listOf(
      ImageResource(src = "file:///sys/icons/test-yellow.svg", type = "image/svg+xml")
    )
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/publish", routes = listOf("file://$mmid/publish"), title = "将服务发布到公网中", permissionType = emptyList()
      )
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// 该接口仅供测试
    routes(
      // 将服务发布到公网
      "/publish" bind HttpMethod.Get by defineStringResponse {
        "发布成功 ${datetimeNow()}"
      },
      "/unPublish" bind HttpMethod.Get by defineStringResponse {
        deletePermission(ipc.remote.mmid, dweb_permissions.first().pid!!)
        "权限已回撤 ${datetimeNow()}"
      }
    ).protected("applicant.test.permission.sys.dweb")
  }

  override suspend fun _shutdown() {
  }

}

class PermissionApplicantTMM :
  NativeMicroModule("applicant.test.permission.sys.dweb", "Permission Applicant") {
  init {
    short_name = name
    categories = listOf(
//      MICRO_MODULE_CATEGORY.Application,
    )
    icons = listOf(
      ImageResource(src = "file:///sys/icons/test-pink.svg", type = "image/svg+xml")
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    onRenderer {
      val win = getMainWindow()
      win.state.setFromManifest(this@PermissionApplicantTMM)
      windowAdapterManager.provideRender(wid) { modifier ->
        Column(modifier) {
          var okk by remember {
            mutableStateOf("")
          }
          Row(horizontalArrangement = Arrangement.SpaceAround) {
            ElevatedButton(onClick = {
              ioAsyncScope.launch {
                okk =
                  nativeFetch("file://provider.test.permission.sys.dweb/publish").body.toPureString()
              }
            }) {
              Text("发布")
            }
            ElevatedButton(onClick = {
              ioAsyncScope.launch {
                okk =
                  nativeFetch("file://provider.test.permission.sys.dweb/unPublish").body.toPureString()
              }
            }) {
              Text("撤销")
            }
          }
          Text(text = okk)
        }
      }
    }
  }

  override suspend fun _shutdown() {
  }
}
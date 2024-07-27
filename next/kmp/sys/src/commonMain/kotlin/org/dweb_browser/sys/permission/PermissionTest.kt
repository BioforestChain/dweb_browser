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
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.ext.deletePermission
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.sys.permission.ext.requestSystemPermissions
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

class PermissionProviderTNN :
  NativeMicroModule("provider.test.permission.sys.dweb", "Permission Provider") {
  init {
    short_name = name
    categories = listOf(MICRO_MODULE_CATEGORY.Service)
    icons = listOf(
      ImageResource(src = "file:///sys/sys-icons/test-yellow.svg", type = "image/svg+xml")
    )
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/publish",
        routes = listOf("file://$mmid/publish"),
        title = "将服务发布到公网中",
      )
    )
  }

  inner class PermissionProviderRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {

    override suspend fun _bootstrap() {
      /// 该接口仅供测试
      routes(
        // 将服务发布到公网
        "/publish" bind PureMethod.GET by defineStringResponse {
          "发布成功 ${datetimeNow()}"
        }, "/unPublish" bind PureMethod.GET by defineStringResponse {
          val res = deletePermission(ipc.remote.mmid, dweb_permissions.first().pid!!)
          "权限已回撤 $res/${datetimeNow()}"
        }).protected("applicant.test.permission.sys.dweb")
    }

    override suspend fun _shutdown() {
    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    PermissionProviderRuntime(bootstrapContext)
}

class PermissionApplicantTMM :
  NativeMicroModule("applicant.test.permission.sys.dweb", "Permission Applicant") { init {
  short_name = name
  categories = listOf(MICRO_MODULE_CATEGORY.Application)
  icons = listOf(
    ImageResource(src = "file:///sys/sys-icons/test-pink.svg", type = "image/svg+xml")
  )
  display = DisplayMode.Fullscreen
}

  inner class PermissionApplicantRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {

    override suspend fun _bootstrap() {
      onRenderer {
        val win = getMainWindow()
        win.setStateFromManifest(manifest)
        windowAdapterManager.provideRender(wid) { modifier ->
          Column(modifier) {
            var okk by remember {
              mutableStateOf("")
            }

            suspend fun PureResponse.getResult() =
              "status=${status.value} ${status.description}\n" + "body=${body.toPureString()}"
            Row(horizontalArrangement = Arrangement.SpaceAround) {
              ElevatedButton(onClick = {
                scopeLaunch(cancelable = true) {
                  okk = nativeFetch("file://provider.test.permission.sys.dweb/publish").getResult()
                }
              }) {
                Text("发布")
              }
              ElevatedButton(onClick = {
                scopeLaunch(cancelable = true) {
                  okk =
                    nativeFetch("file://provider.test.permission.sys.dweb/unPublish").getResult()
                }
              }) {
                Text("撤销")
              }
            }
            Row {
              ElevatedButton(onClick = {
                scopeLaunch(cancelable = false) {
                  okk = requestSystemPermissions(
                    SystemPermissionTask(
                      SystemPermissionName.CAMERA,
                      title = "测试获取拍照权限",
                      description = "相机权限申请相机权限申请相机权限申请相机权限申请相机权限申请相机权限申请"
                    ), SystemPermissionTask(
                      SystemPermissionName.CALL,
                      "测试获取电话权限",
                      description = "电话权限申请电话权限申请电话权限申请电话权限申请电话权限申请电话权限申请"
                    ), SystemPermissionTask(
                      SystemPermissionName.LOCATION,
                      "测试获取定位",
                      description = "定位权限申请定位权限申请定位权限申请定位权限申请定位权限申请定位权限申请"
                    ), SystemPermissionTask(
                      SystemPermissionName.PHONE, "测试获取设备信息", description = "获取设备信息"
                    )
                  ).let { Json.encodeToString(it) }
                }
              }) {
                Text("系统权限")
              }
            }
            Row(horizontalArrangement = Arrangement.SpaceAround) {
              ElevatedButton(onClick = {
                scopeLaunch(cancelable = true) {
                  okk = nativeFetch("file://geolocation.sys.dweb/location").getResult()
                }
              }) {
                Text("定位权限")
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

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    PermissionApplicantRuntime(bootstrapContext)
}
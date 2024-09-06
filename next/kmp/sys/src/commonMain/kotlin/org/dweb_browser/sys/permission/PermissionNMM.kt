package org.dweb_browser.sys.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.core.std.permission.AuthorizationRecord
import org.dweb_browser.core.std.permission.PermissionHooks
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.core.std.permission.permissionStdProtocol
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.render.PermissionManagerRender
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.render.AppLogo

class PermissionNMM : NativeMicroModule("permission.sys.dweb", PermissionI18nResource.name.text) {
  init {
    short_name = PermissionI18nResource.short_name.text;
    dweb_protocols = listOf("permission.std.dweb")
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(src = "file:///sys/sys-icons/$mmid.svg", type = "image/svg+xml")
    )
    BuildinPermission.start()
  }

  inner class PermissionRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      val permissionTable = permissionStdProtocol(hooks)

      routes("/request" bind PureMethod.POST by defineJsonResponse {
        val permissions = request.body.toPureString()
        debugPermission("request@sys", "ipc=>${ipc.remote.mmid}, permission=>$permissions")
        val permissionTaskList = Json.decodeFromString<List<SystemPermissionTask>>(permissions)
        requestSysPermission(
          microModule = getRemoteRuntime(), // requestMicroModule,
          pureViewController = null,//getOrOpenMainWindow().apply { hide() }.pureViewControllerState.value,
          permissionTaskList = permissionTaskList
        ).toJsonElement()
      })

      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
          state.keepBackground = true/// 保持在后台运行
          windowAdapterManager.provideRender(id) { modifier ->
            PermissionManagerRender(modifier, this, permissionTable)
          }
        }
      }
    }

    override suspend fun _shutdown() {}

    @OptIn(ExperimentalMaterial3Api::class)
    val hooks = object : PermissionHooks {
      override suspend fun onRequestPermissions(
        applicantIpc: Ipc, permissions: List<PermissionProvider>,
      ): Map<PermissionProvider, AuthorizationRecord> {
        val applicant = applicantIpc.remote
        val permissionModuleMap = permissions.mapNotNull { permission ->
          bootstrapContext.dns.query(permission.providerMmid)?.let { permission to it }
        }.toMap()
        val resultMap = mutableMapOf<PermissionProvider, Boolean>()
        val checkedMap = mutableMapOf<PermissionProvider, MutableState<Boolean>>()
        val submitDeferred = CompletableDeferred<Unit>()

        val modal = createBottomSheets(
          title = "${applicant.name} ${PermissionI18nResource.request_title.text}",
          iconUrl = icons.first().src
        ) {
          Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.Center) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = PermissionI18nResource.request_title.text,
                  style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(32.dp))

                AppLogo.fromResources(
                  applicant.icons, fetchHook = blobFetchHook, base = AppLogo(errorContent = {
                    Text(applicant.short_name)
                  })
                ).toIcon().Render(Modifier.size(32.dp))

                Spacer(Modifier.width(16.dp))

                Text(applicant.mmid, style = MaterialTheme.typography.bodySmall)
              }

              HorizontalDivider()

              for ((permission, module) in permissionModuleMap) {
                ListItem(colors = ListItemDefaults.colors(
                  containerColor = Color.Transparent,
                  overlineColor = Color.Transparent,
                ),
                  leadingContent = {
                    BadgedBox(badge = {
                      AppLogo.pickFrom(permission.badges, fetchHook = blobFetchHook)
                        .Render(Modifier.size(6.dp))
                    }) {
                      AppLogo.fromResources(
                        module.icons, fetchHook = blobFetchHook, base = AppLogo(
                          description = module.name,
                          errorContent = { Image(Icons.Rounded.Info, module.name) },
                        )
                      ).toIcon().Render(Modifier.size(24.dp))
                    }
                  },
                  headlineContent = { Text(permission.title()) },
                  supportingContent = permission.description?.let {
                    { Text(it()) }
                  },
                  trailingContent = {
                    var checked by remember {
                      checkedMap.getOrPut(permission) {
                        mutableStateOf(true)
                      }
                    }
                    val icon: (@Composable () -> Unit)? = if (checked) {
                      @Composable {
                        Icon(
                          imageVector = Icons.Filled.Check,
                          contentDescription = null,
                          modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                      }
                    } else null
                    Switch(checked = checked, onCheckedChange = {
                      checked = it
                    }, thumbContent = icon)
                  })
              }
              HorizontalDivider()
              Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Button(colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ), onClick = {
                  for (permission in checkedMap.keys) {
                    resultMap[permission] = false
                  }
                  submitDeferred.complete(Unit)
                }) {
                  Text(text = PermissionI18nResource.request_button_refuse())
                }

                Row {
                  Button(onClick = {
                    for ((permission, state) in checkedMap) {
                      resultMap[permission] = state.value
                    }
                    submitDeferred.complete(Unit)
                  }) {
                    Text(text = PermissionI18nResource.request_button_confirm())
                  }
                  Spacer(Modifier.width(8.dp))
                  ElevatedButton(onClick = {
                    for (permission in checkedMap.keys) {
                      resultMap[permission] = true
                    }
                    submitDeferred.complete(Unit)
                  }) {
                    Text(text = PermissionI18nResource.request_button_authorize_all())
                  }
                }
              }
            }
          }
        }
        submitDeferred.invokeOnCompletion {
          scopeLaunch(cancelable = false) {
            modal.close()
          }
        }
        // 关闭主窗口，显示modal
        val mainWindow = getMainWindow()
        mainWindow.hide()
        modal.open()
        // 等待关闭
        modal.onClose.awaitOnce()
        modal.destroy()
        mainWindow.closeRoot()
        return resultMap.mapValues { it.key.getAuthorizationRecord(it.value, applicant.mmid) }
      }
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    PermissionRuntime(bootstrapContext)
}
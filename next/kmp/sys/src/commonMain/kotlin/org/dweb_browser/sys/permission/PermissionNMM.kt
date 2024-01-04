package org.dweb_browser.sys.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationRecord
import org.dweb_browser.core.std.permission.PermissionHooks
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.core.std.permission.permissionStdProtocol
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.compose.HorizontalDivider
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.sys.SysI18nResource
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import org.dweb_browser.sys.window.ext.openMainWindow
import org.dweb_browser.sys.window.render.AppIcon

class PermissionNMM : NativeMicroModule("permission.sys.dweb", "Permission Management") {
  init {
    short_name = "Permission";
    dweb_protocols = listOf("permission.std.dweb")
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Hub_Service
    )
    icons = listOf(
      ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml")
    )
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {

    val iconFetchHook: FetchHook = {

      val response = nativeFetch(request.url)
      returnResponse(response)
    }
    val hooks = object : PermissionHooks {
      override suspend fun onRequestPermissions(
        applicantIpc: Ipc, permissions: List<PermissionProvider>
      ): Map<PermissionProvider, AuthorizationRecord> {
        val applicant = applicantIpc.remote
        val applicantMmid = applicant.mmid
        val permissionModuleMap = permissions.mapNotNull { permission ->
          bootstrapContext.dns.query(permission.providerMmid)?.let { permission to it }
        }.toMap()
        val resultMap = mutableMapOf<PermissionProvider, Boolean>()
        val checkedMap = mutableMapOf<PermissionProvider, MutableState<Boolean>>()
        val submitDeferred = CompletableDeferred<Unit>()

        val modal = createBottomSheets(
          title = "${applicant.name} ${SysI18nResource.permission_title_request.text}",
          iconUrl = "file:///sys/icons/$mmid.svg"
        ) {
          Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.Center) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = SysI18nResource.permission_title_request.text,
                  style = MaterialTheme.typography.titleLarge
                )
                val applicantIcon = remember {
                  applicant.icons.toStrict().pickLargest()
                }
                Spacer(Modifier.width(32.dp))
                when (applicantIcon) {
                  null -> Text(applicant.short_name)
                  else -> Box(Modifier.size(32.dp)) {
                    AppIcon(
                      applicantIcon,
                      iconFetchHook = iconFetchHook,
                    )
                  }
                }

                Spacer(Modifier.width(16.dp))

                Text(applicant.mmid, style = MaterialTheme.typography.bodySmall)
              }

              HorizontalDivider()

              for ((permission, module) in permissionModuleMap) {
                ListItem(
                  colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    overlineColor = Color.Transparent,
                  ),
                  leadingContent = {
                    BadgedBox(badge = {
                      val badgeIcon = remember {
                        permission.badges.pickLargest()
                      }
                      badgeIcon?.also {
                        AppIcon(
                          iconResource = it,
                          modifier = Modifier.size(6.dp),
                          iconFetchHook = iconFetchHook,
                        )
                      }
                    }) {
                      val providerIcon = remember {
                        module.icons.toStrict().pickLargest()
                      }
                      when (providerIcon) {
                        null -> Image(
                          imageVector = Icons.Rounded.Info, contentDescription = module.name
                        )

                        else -> AppIcon(
                          iconResource = providerIcon,
                          modifier = Modifier.size(24.dp),
                          iconDescription = module.name,
                          iconFetchHook = iconFetchHook,
                        )
                      }
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
                Button(
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                  ),
                  onClick = {
                    for (permission in checkedMap.keys) {
                      resultMap[permission] = false
                    }
                    submitDeferred.complete(Unit)
                  }) {
                  Text(text = SysI18nResource.permission_button_refuse())
                }

                Row {
                  Button(onClick = {
                    for ((permission, state) in checkedMap) {
                      resultMap[permission] = state.value
                    }
                    submitDeferred.complete(Unit)
                  }) {
                    Text(text = SysI18nResource.permission_button_confirm())
                  }
                  Spacer(Modifier.width(8.dp))
                  ElevatedButton(onClick = {
                    for (permission in checkedMap.keys) {
                      resultMap[permission] = true
                    }
                    submitDeferred.complete(Unit)
                  }) {
                    Text(text = SysI18nResource.permission_button_authorize_all())
                  }
                }
              }
            }
          }
        }
        submitDeferred.invokeOnCompletion {
          ioAsyncScope.launch {
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
        return resultMap.mapValues { it.key.getAuthorizationRecord(it.value, applicantMmid) }
      }
    }
    val table = permissionStdProtocol(hooks)

    routes(
      "/request" bind PureMethod.POST by defineJsonResponse {
        val permissions = request.body.toPureString()
        debugPermission("request@sys", "ipc=>${ipc.remote.mmid}, permission=>$permissions")
        val permissionTaskList = Json.decodeFromString<List<SystemPermissionTask>>(permissions)
        val windowController = getOrOpenMainWindow().apply {
          this.show(); this.enableAlwaysOnTop(); this.focus()
        }
        //val requestMicroModule = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@PermissionNMM
        val result = requestSysPermission(
          microModule = this@PermissionNMM, // requestMicroModule,
          pureViewController = windowController.pureViewControllerState.value,
          permissionTaskList = permissionTaskList
        )
        windowController.hide()
        result.toJsonElement()
      }
    )

    onRenderer {
      getMainWindow().apply {
        state.setFromManifest(this@PermissionNMM)
        windowAdapterManager.provideRender(id) { modifier ->
          PermissionManagerRender(modifier, this, table)
        }
      }
    }
  }

  override suspend fun _shutdown() {

  }
}
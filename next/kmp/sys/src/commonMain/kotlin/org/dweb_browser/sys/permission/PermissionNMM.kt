package org.dweb_browser.sys.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.AuthorizationRecord
import org.dweb_browser.core.std.permission.PermissionHooks
import org.dweb_browser.core.std.permission.PermissionProvider
import org.dweb_browser.core.std.permission.permissionStdProtocol
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.setFromManifest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.ext.createBottomSheets
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
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
          title = "${applicant.name} 申请权限", iconUrl = "file:///sys/icons/$mmid.svg"
        ) {
          Card(elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
            Column(verticalArrangement = Arrangement.Center) {
              Box(Modifier.size(32.dp)) {
                val applicantIcon = remember {
                  applicant.icons.toStrict().pickLargest()
                }
                when (applicantIcon) {
                  null -> Text(applicant.short_name)
                  else -> AppIcon(applicantIcon)
                }
              }

//              HorizontalDivider()
              Divider()

              for ((permission, module) in permissionModuleMap) {
                ListItem(leadingContent = {
                  BadgedBox(badge = {
                    val badgeIcon = remember {
                      permission.badges.pickLargest()
                    }
                    badgeIcon?.also {
                      AppIcon(iconResource = it, Modifier.size(6.dp))
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
                        iconDescription = module.name
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
//              HorizontalDivider()
              Divider()
              Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = {
                  for (permission in checkedMap.keys) {
                    resultMap[permission] = false
                  }
                  submitDeferred.complete(Unit)
                }) {
                  Text(text = "拒绝")
                }

                Button(onClick = {
                  for ((permission, state) in checkedMap) {
                    resultMap[permission] = state.value
                  }
                  submitDeferred.complete(Unit)
                }) {
                  Text(text = "确定")
                }

                ElevatedButton(onClick = {
                  for (permission in checkedMap.keys) {
                    resultMap[permission] = true
                  }
                  submitDeferred.complete(Unit)
                }) {
                  Text(text = "授权全部")
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
        // 等待关闭
        modal.open()
        modal.onClose.awaitOnce()
        modal.destroy()
        return resultMap.mapValues { it.key.getAuthorizationRecord(it.value, applicantMmid) }
      }
    }
    permissionStdProtocol(hooks)

    onRenderer {
      getMainWindow().state.setFromManifest(this@PermissionNMM)
    }
  }

  override suspend fun _shutdown() {

  }
}
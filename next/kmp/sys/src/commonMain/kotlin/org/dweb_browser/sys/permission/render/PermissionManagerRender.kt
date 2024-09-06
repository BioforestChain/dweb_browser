package org.dweb_browser.sys.permission.render

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.permission.AuthorizationRecord
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PermissionTable
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.compose.ScalePopupContent
import org.dweb_browser.helper.compose.ScalePopupPlaceholder
import org.dweb_browser.sys.permission.PermissionI18nResource
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText

@Composable
fun PermissionManagerRender(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
  table: PermissionTable,
) {
  windowRenderScope.WindowContentScaffoldWithTitleText(topBarTitleText = PermissionI18nResource.record_list_title()) { innerPadding ->
    val allData = table.AllData().also {
      if (it.isEmpty()) {
        NoDataRender(PermissionI18nResource.no_record())
      }
    }
    LazyColumn(contentPadding = innerPadding) {
      items(allData) { item ->
        ListItem(
          headlineContent = {
            Text(item.applicantMmid)
          },
          supportingContent = {
            Text(item.permissionId)
          },
          trailingContent = {
            var isShowMenu by remember { mutableStateOf(false) }
            val authorizationStatus = item.record.safeStatus
            IconButton(
              { isShowMenu = true },
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isShowMenu) {
                  authorizationStatus.Color().copy(alpha = 0.2f)
                } else Color.Transparent
              )
            ) {
              Icon(
                when (authorizationStatus) {
                  AuthorizationStatus.GRANTED -> Icons.Rounded.GppGood
                  AuthorizationStatus.UNKNOWN -> Icons.Rounded.GppMaybe
                  AuthorizationStatus.DENIED -> Icons.Rounded.GppBad
                },
                contentDescription = authorizationStatus.name,
                tint = authorizationStatus.Color()
              )
              ScalePopupPlaceholder(windowRenderScope.scale) {
                PermissionMenu(
                  isShowMenu = isShowMenu,
                  onCloseMenu = { isShowMenu = false },
                  table = table,
                  item = item,
                )
              }
            }// IconButton
          }
        )
      }
    }
  }
}

@Composable
private fun PermissionMenu(
  isShowMenu: Boolean,
  onCloseMenu: () -> Unit,
  table: PermissionTable,
  item: PermissionTable.PermissionRow,
) {
  val scope = rememberCoroutineScope()
  fun generateAuthorizationRecord(granted: Boolean?) {
    scope.launch {
      launch {
        delay(150)
        onCloseMenu()
      }
      table.addRecord(
        AuthorizationRecord.generateAuthorizationRecord(
          item.permissionId,
          item.applicantMmid,
          granted
        )
      )
    }
  }

  DropdownMenu(isShowMenu, onDismissRequest = onCloseMenu) {
    ScaleDropdownMenuItem(
      onClick = {
        generateAuthorizationRecord(true)
      },
      leadingIcon = {
        Icon(
          Icons.Rounded.GppGood,
          contentDescription = AuthorizationStatus.GRANTED.name
        )
      },
      text = {
        Text(PermissionI18nResource.record_state_granted())
      },
      colors = MenuDefaults.itemColors(leadingIconColor = AuthorizationStatus.GRANTED.Color())
    )
    ScaleDropdownMenuItem(
      onClick = {
        generateAuthorizationRecord(null)
      },
      leadingIcon = {
        Icon(
          Icons.Rounded.GppMaybe,
          contentDescription = AuthorizationStatus.UNKNOWN.name
        )
      },
      text = {
        Text(PermissionI18nResource.record_state_unknown())
      },
      colors = MenuDefaults.itemColors(leadingIconColor = AuthorizationStatus.UNKNOWN.Color())
    )
    ScaleDropdownMenuItem(
      onClick = {
        generateAuthorizationRecord(false)
      },
      leadingIcon = {
        Icon(
          Icons.Rounded.GppBad,
          contentDescription = AuthorizationStatus.DENIED.name
        )
      },
      text = {
        Text(PermissionI18nResource.record_state_denied())
      },
      colors = MenuDefaults.itemColors(leadingIconColor = AuthorizationStatus.DENIED.Color())
    )
    HorizontalDivider()
    ScaleDropdownMenuItem(
      onClick = {
        scope.launch {
          table.removeRecord(
            item.record.providerMmid,
            item.permissionId,
            item.applicantMmid
          )
        }
      },
      leadingIcon = {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = "remove record"
        )
      },
      text = {
        Text(PermissionI18nResource.remove_record())
      },
    )
  }
}

@Composable
private fun ScaleDropdownMenuItem(
  text: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  enabled: Boolean = true,
  colors: MenuItemColors = MenuDefaults.itemColors(),
  contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
  interactionSource: MutableInteractionSource? = null,
) {
  ScalePopupContent {
    DropdownMenuItem(
      text = text,
      onClick = onClick,
      modifier = modifier,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      enabled = enabled,
      colors = colors,
      contentPadding = contentPadding,
      interactionSource = interactionSource,
    )
  }
}


@Composable
fun AuthorizationStatus.Color() = when (this) {
  AuthorizationStatus.GRANTED -> MaterialTheme.colorScheme.primary
  AuthorizationStatus.UNKNOWN -> MaterialTheme.colorScheme.secondary
  AuthorizationStatus.DENIED -> MaterialTheme.colorScheme.error
}
package org.dweb_browser.browser.desk.version

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.R
import org.dweb_browser.browser.desk.debugDesk

/**
 * 显示更新界面
 */
@Composable
internal fun NewVersionView() {
  val newVersionItem = NewVersionModel.versionItem.value ?: return
  val versionType = NewVersionModel.versionType
  AnimatedContent(
    targetState = versionType.value, label = ""
  ) { type ->
    when (type) {
      VersionType.NewVersion -> {
        DialogNewVersion(
          newVersionItem = newVersionItem,
          onUpgrade = { NewVersionModel.updateVersionType(VersionType.Download) },
          onCancel = { NewVersionModel.updateVersionType(VersionType.Hide) }
        )
      }

      VersionType.Download -> {
        DialogDownloadView(
          url = newVersionItem.android,
          onCancel = { NewVersionModel.updateVersionType(VersionType.Hide) },
        )
      }

      VersionType.Install -> {
        DialogInstallView { NewVersionModel.updateVersionType(VersionType.Hide) }
      }

      VersionType.Hide -> {}
    }
  }
}

@Composable
private fun DialogNewVersion(
  newVersionItem: NewVersionItem, onUpgrade: () -> Unit, onCancel: () -> Unit
) {
  val scope = rememberCoroutineScope()
  AlertDialog(
    onDismissRequest = { /* 新版本界面，不允许关闭，需要点击按钮 */ },
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = BrowserI18nResource.dialog_version_title(),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = newVersionItem.version,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.outline
        )
        Image(
          imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_foreground),
          contentDescription = null,
          modifier = Modifier.size(64.dp)
        )
      }
    },
    text = { Text(text = BrowserI18nResource.dialog_upgrade_description()) },
    confirmButton = {
      Button(onClick = {
        scope.launch { onUpgrade() }
      }) {
        Text(BrowserI18nResource.dialog_upgrade_button_upgrade())
      }
    },
    dismissButton = {
      Button(
        onClick = { onCancel() }) {
        Text(BrowserI18nResource.dialog_upgrade_button_delay())
      }
    }
  )
}

@Composable
private fun DialogDownloadView(url: String, onCancel: () -> Unit) {
  var current by remember { mutableLongStateOf(0L) }
  var total by remember { mutableLongStateOf(0L) }
  LaunchedEffect(Unit) {
    NewVersionModel.download(
      url = url,
      callback = { c, t -> current = c; total = t; },
    )
  }
  AlertDialog(
    onDismissRequest = { /* 下载中，不允许关闭，需要点击按钮 */ },
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = BrowserI18nResource.dialog_downloading_title(),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
          Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
          )
          val backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          Canvas(modifier = Modifier.size(64.dp)) {
            drawCircle(color = backgroundColor, style = Stroke(2f))
            drawArc(
              color = backgroundColor,
              startAngle = -90f,
              sweepAngle = if (current == 0L) -360f else ((current * 1.0f / total) - 1) * 360f,
              useCenter = true
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        // 转移到后台下载
        onCancel()
      }) {
        Text(BrowserI18nResource.dialog_upgrade_button_background())
      }
    },
  )
}

/**
 * 在 Install 这边判断系统是否有安装权限
 * 有： 直接关闭当前界面，并打开安装界面
 * 无： 显示需要授权，按钮是打开授权界面
 */
@Composable
private fun DialogInstallView(onCancel: () -> Unit) {
  if (NewVersionModel.apkFile == null) {
    debugDesk("DialogInstallView", "apkFile is null")
    onCancel()
    return
  } else if (NewVersionModel.checkInstallPermission()) {
    debugDesk("DialogInstallView", "installApk")
    // 唤醒安装，并关闭
    NewVersionModel.installApk()
    onCancel()
    return
  }
  AlertDialog(
    onDismissRequest = { /* 安装界面，不允许关闭，需要点击按钮 */ },
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = BrowserI18nResource.dialog_install_title(),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    },
    text = {
      Text(text = BrowserI18nResource.dialog_install_description())
    },
    confirmButton = {
      Button(onClick = { NewVersionModel.openInstallPermissionSetting() }) {
        Text(BrowserI18nResource.dialog_upgrade_button_setting())
      }
    },
  )
}
package org.dweb_browser.browser.desk.upgrade

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.model.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewVersionController.Render() {
  if (newVersionType == NewVersionType.Hide) return
  if (canOpen) {
    LaunchedEffect(Unit) {
      canOpen = false
      newVersionItem?.let { item ->
        if (item.status.state == DownloadState.Completed) {
          newVersionType = NewVersionType.Install
        }
      }
    }
  }

  BasicAlertDialog(
    onDismissRequest = {
      if (newVersionItem?.forceUpdate != true) {
        newVersionType = NewVersionType.Hide
      } else {
        // 如果是强制更新，就不能响应点击空白区域关闭升级对话框
      }
    },
  ) { // 里面就是具体显示的内容了
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center
    ) {
      AnimatedContent(
        targetState = newVersionType, label = "",
        transitionSpec = { // 实现左出右进
          if (targetState > initialState) {
            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
              slideOutHorizontally { width -> -width } + fadeOut()
            )
          } else {
            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
              slideOutHorizontally { width -> width } + fadeOut()
            )
          }
        }
      ) { type ->
        when (type) {
          NewVersionType.NewVersion -> DialogNewVersion()

          NewVersionType.Download -> DialogDownloadView()

          NewVersionType.Install -> DialogInstallView()

          NewVersionType.Hide -> {}
        }
      }
    }
  }
}

@Composable
private fun NewVersionController.DialogNewVersion() {
  val newVersion = newVersionItem ?: return
  DialogContent(
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
          text = newVersion.versionName,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.outline
        )
        Image(
          painter = BrowserDrawResource.Logo.painter(),
          contentDescription = null,
          modifier = Modifier.size(64.dp)
        )
      }
    },
    text = { Text(text = BrowserI18nResource.dialog_upgrade_description()) },
    confirmButton = {
      Button(onClick = {
        if (newVersion.status.state == DownloadState.Completed) {
          newVersionType = NewVersionType.Install
        } else {
          newVersionType = NewVersionType.Download
        }
      }) {
        Text(BrowserI18nResource.dialog_upgrade_button_upgrade())
      }
    },
    dismissButton = {
      Button(
        onClick = { newVersionType = NewVersionType.Hide }) {
        Text(BrowserI18nResource.dialog_upgrade_button_delay())
      }
    }
  )
}

@Composable
private fun NewVersionController.DialogDownloadView() {
  val newVersion = newVersionItem ?: return
  SideEffect {
    deskNMM.scopeLaunch(cancelable = true) { downloadApp() }
  }
  DialogContent(
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
            painter = BrowserDrawResource.Logo.painter(),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
          )
          val backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          Canvas(modifier = Modifier.size(64.dp)) {
            drawCircle(color = backgroundColor, style = Stroke(2f))
            drawArc(
              color = backgroundColor,
              startAngle = -90f,
              sweepAngle = if (newVersion.status.current == 0L) {
                -360f
              } else {
                (newVersion.progress() - 1) * 360f
              },
              useCenter = true
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { newVersionType = NewVersionType.Hide /* 转移到后台下载 */ }) {
        Text(BrowserI18nResource.dialog_upgrade_button_background())
      }
    }
  )
}

/**
 * 在 Install 这边判断系统是否有安装权限
 * 有： 直接关闭当前界面，并打开安装界面
 * 无： 显示需要授权，按钮是打开授权界面
 */
@Composable
private fun NewVersionController.DialogInstallView() {
  DialogContent(
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
      Button(onClick = {
        newVersionType = NewVersionType.Hide
        openSystemInstallSetting()
      }) {
        Text(BrowserI18nResource.dialog_upgrade_button_setting())
      }
    },
  )
}

@Composable
private fun DialogContent(
  title: @Composable () -> Unit,
  text: @Composable (() -> Unit)? = null,
  dismissButton: @Composable (() -> Unit)? = null,
  confirmButton: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    title()
    text?.let { text() }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.End),
      verticalAlignment = Alignment.CenterVertically
    ) {
      dismissButton?.let { it() }
      confirmButton()
    }
  }
}
package org.dweb_browser.browser.about

import android.webkit.WebView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.allFeatures
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.theme.LocalColorful
import org.dweb_browser.sys.device.DeviceManage
import org.dweb_browser.sys.device.model.Battery
import org.dweb_browser.sys.device.model.DeviceData
import org.dweb_browser.sys.device.model.DeviceInfo
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.LocalWindowController

data class AndroidSystemInfo(
  val os: String = "Android",
  val osVersion: String,
//  val deviceName: String,
  val sdkInt: Int,
)

actual suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID) {
  val deviceData = DeviceInfo.deviceData
  val batteryInfo = DeviceInfo.getBatteryInfo()
  val androidSystemInfo = AndroidSystemInfo(
    osVersion = DeviceInfo.osVersion,
//    deviceName = deviceData.deviceName,
    sdkInt = DeviceInfo.sdkInt
  )
  val appInfo = AboutAppInfo(
    appVersion = DeviceManage.deviceAppVersion(),
    webviewVersion = WebView.getCurrentWebViewPackage()?.versionName ?: "Unknown"
  )
  provideAboutRender(id) { modifier ->
    AboutRender(
      modifier = modifier,
      appInfo = appInfo,
      androidSystemInfo = androidSystemInfo,
      deviceData = deviceData,
      batteryInfo = batteryInfo,
    )
  }
}

@Composable
fun AboutRender(
  modifier: Modifier,
  appInfo: AboutAppInfo,
  androidSystemInfo: AndroidSystemInfo,
  deviceData: DeviceData,
  batteryInfo: Battery,
) {
  LazyColumn(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top,
  ) {
    item("app-info") {
      AboutTitle(AboutI18nResource.app())
      AboutAppInfoRender(appInfo, webViewFeaturesContent = {
        val nav = LocalWindowController.current.navigation
        AboutDetailsNav(
          onClick = {
            nav.pushPage { modifier ->
              WebViewFeaturesRender(modifier)
            }
          },
          labelName = AboutI18nResource.webview(),
          text = "${DWebView.allFeatures.enabledFeatures.size}/${DWebView.allFeatures.size}"
        )
      })
      AboutHorizontalDivider()
    }
    item("system-info") {
      AboutTitle(AboutI18nResource.system())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.os(), text = androidSystemInfo.os
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.osVersion(), text = androidSystemInfo.osVersion
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.sdkInt(), text = androidSystemInfo.sdkInt.toString()
        )
//        AboutDetailsItem(
//          labelName = AboutI18nResource.deviceName(), text = androidSystemInfo.deviceName
//        )
      }
      AboutHorizontalDivider()
    }
    item("hardware-info") {
      AboutTitle(AboutI18nResource.hardware())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.brand(), text = deviceData.brand
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.modelName(), text = deviceData.deviceModel
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.hardware(), text = deviceData.hardware
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.supportAbis(), text = deviceData.supportAbis
        )
        AboutDetailsItem(
          labelName = "ID", text = deviceData.id
        )
        AboutDetailsItem(
          labelName = "DISPLAY", text = deviceData.display
        )
        AboutDetailsItem(
          labelName = "BOARD", text = deviceData.board
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.manufacturer(), text = deviceData.manufacturer
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.display(), text = deviceData.screenSizeInches
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.resolution(), text = deviceData.resolution
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.density(), text = deviceData.density
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.refreshRate(), text = deviceData.refreshRate
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.memory(),
          text = "${deviceData.memory!!.usage}/${deviceData.memory!!.total}"
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.storage(),
          text = "${deviceData.storage!!.internalUsageSize}/${deviceData.storage!!.internalTotalSize}"
        )
      }
      AboutHorizontalDivider()
    }
    item("battery-info") {
      AboutTitle(AboutI18nResource.battery())
      AboutColumnContainer {
        AboutDetailsItem(
          labelName = AboutI18nResource.status(),
          text = if (batteryInfo.isPhoneCharging) AboutI18nResource.charging() else AboutI18nResource.discharging()
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.health(), text = batteryInfo.batteryHealth ?: "Unknown"
        )
        AboutDetailsItem(
          labelName = AboutI18nResource.percent(), text = "${batteryInfo.batteryPercent}%"
        )
      }
      AboutHorizontalDivider()
    }
    item("env-switch") {
      EnvSwitcherRender()
      AboutHorizontalDivider()
    }
  }
}

@Composable
fun WindowContentRenderScope.WebViewFeaturesRender(modifier: Modifier) {
  AboutPage(modifier = modifier, title = AboutI18nResource.webview()) { pageContentModifier ->
    LazyColumn(pageContentModifier) {
      items(DWebView.allFeatures.groups) { group ->
        AboutTitle(group.i18n())
        AboutColumnContainer {
          val startIndex = remember(group) {
            DWebView.allFeatures.groups.run { slice(0..<indexOf(group)).flatten().size }
          }
          group.forEachIndexed { index, feature ->
            var showId by remember { mutableStateOf(false) }
            AboutDetailsBase(Modifier.wrapContentHeight().clickable { showId = !showId }) {
              Row(Modifier.padding(start = 4.dp).weight(1f)) {
                Box(Modifier.requiredSize(16.dp), contentAlignment = Alignment.CenterEnd) {
                  Text(
                    "${startIndex + index + 1}.".padStart(3, '0'),
                    style = MaterialTheme.typography.bodySmall.run { copy(fontSize = fontSize * 0.8f) },
                  )
                }
                Column(Modifier.padding(horizontal = 4.dp).animateContentSize()) {
                  Text(
                    feature.i18n(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (showId) {
                    Text(
                      feature.featureId,
                      style = MaterialTheme.typography.bodySmall,
                      color = LocalContentColor.current.copy(0.5f),
                    )
                  }
                }
              }
              Box(
                Modifier.requiredSize(16.dp).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  if (feature.enabled) Icons.Default.Check else Icons.Default.Close,
                  contentDescription = if (feature.enabled) "enabled" else "disabled",
                  tint = if (feature.enabled) LocalColorful.current.Green.current else MaterialTheme.colorScheme.error
                )
              }
            }
          }
          AboutHorizontalDivider()
        }
      }
    }
  }
}

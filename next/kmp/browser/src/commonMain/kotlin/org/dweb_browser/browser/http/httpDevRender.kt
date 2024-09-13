package org.dweb_browser.browser.http

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.dwebHttpGatewayService
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.core.std.http.DWEB_PING_URI
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import squircleshape.SquircleShape

fun HttpNMM.installDevRenderer() {
  runtimeFlow.collectIn { runtime ->
    if (runtime is HttpNMM.HttpRuntime) {
      if (runtime.categories.contains(MICRO_MODULE_CATEGORY.Application)) {
        runtime.installDevRenderer()
      }
    }
  }
}

private fun HttpNMM.HttpRuntime.installDevRenderer() {
  onRenderer {
    val win = getMainWindow()
    win.setStateFromManifest(this@installDevRenderer)
    /// 提供渲染适配
    windowAdapterManager.provideRender(wid) { modifier ->
      WindowContentScaffoldWithTitleText(modifier, topBarTitleText = "网络控制中心") {
        LazyColumn(modifier = Modifier.padding(it).padding(horizontal = 16.dp)) {
          item("gateway") {
            val currentPort by dwebHttpGatewayService.server.stateFlow.collectAsState()
            Text("当前网关端口号: $currentPort")
            var customPort by remember {
              mutableStateOf("0")
            }
            TextField(
              value = customPort,
              onValueChange = { customPort = it },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(onClick = {
              scopeLaunch(cancelable = true) {
                dwebHttpGatewayService.server.close()
                dwebHttpGatewayService.server.start(customPort.toIntOrNull()?.toUShort() ?: 0u)
              }
            }) {
              Text(
                text = when (currentPort) {
                  null -> "启动网关服务"
                  else -> "重启网关服务"
                }
              )
            }
            HorizontalDivider()
          }
          item("proxy") {
            val currentProxyUrl by dwebProxyService.proxyUrl.collectAsState()
            Text("当前代理地址：$currentProxyUrl")

            Button({
              scopeLaunch(cancelable = true) {
                dwebProxyService.stop()
                dwebProxyService.start()
              }
            }) {
              Text(
                when (currentProxyUrl) {
                  null -> "启动代理服务"
                  else -> "重启代理服务"
                }
              )
            }
            HorizontalDivider()
          }

          item("pingpong") {
            Text("服务保活链接: $DWEB_PING_URI")
            var checkResult by remember { mutableStateOf(AnnotatedString("")) }
            val style = LocalTextStyle.current.toSpanStyle()
            var useDwebRootHost by remember { mutableStateOf(true) }
            Switch(useDwebRootHost, { useDwebRootHost = it })
            var customOrigin by remember { mutableStateOf("https://docs.dweb-browser.org") }
            if (!useDwebRootHost) {
              TextField(customOrigin, { customOrigin = it }, label = { Text("Custom Origin") })
            }
            Button({
              scopeLaunch(cancelable = true) {
                runCatching {
                  val origin = when {
                    useDwebRootHost -> "https://internal.dweb"
                    else -> customOrigin.trim()
                  }
                  val response = client.fetch(
                    PureClientRequest(
                      "$origin$DWEB_PING_URI?now=${datetimeNow()}",
                      method = PureMethod.GET,
                      headers = PureHeaders().apply { init("Sec-Fetch-Dest", "dwebproxy") },
                    ),
                  )
                  checkResult = buildAnnotatedString {
                    withStyle(
                      style.copy(
                        color = if (response.status.value == 200) Color.Green else Color.Red,
                        fontSize = 12.sp
                      )
                    ) {
                      append("[${response.status.value}] ${response.status.description}\n")
                      response.headers.forEach { (k, v) -> append("$k: $v\n") }
                    }
                    withStyle(
                      style.copy(
                        color = when (response.status.value) {
                          200 -> style.color
                          else -> Color.Red.copy(alpha = 0.8f)
                        }, fontSize = 9.sp
                      )
                    ) {
                      append(response.body.toPureString())
                    }
                  }
                }.getOrElse { err ->
                  checkResult = buildAnnotatedString {
                    withStyle(
                      style.copy(
                        color = Color.Red.copy(alpha = 0.8f), fontSize = 9.sp
                      )
                    ) {
                      append(err.stackTraceToString())
                    }
                  }
                }
              }
            }) {
              Text("执行嗅探")
            }
            if (checkResult.isNotEmpty()) {
              Box(
                Modifier.padding(8.dp)
                  .background(MaterialTheme.colorScheme.background, shape = SquircleShape(16.dp))
              ) {
                FilledTonalIconButton(
                  { checkResult = AnnotatedString("") },
                  modifier = Modifier.align(Alignment.TopEnd),
                ) {
                  Icon(Icons.Rounded.Clear, null)
                }
                Text(checkResult, fontSize = 9.sp)
              }
            }
          }
        }
      }
    }
  }
}

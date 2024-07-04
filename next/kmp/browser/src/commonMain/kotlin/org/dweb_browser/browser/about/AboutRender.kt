package org.dweb_browser.browser.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.compose.hex
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.render.LocalWindowControllerTheme

internal fun provideAboutRender(
  wid: UUID,
  render: @Composable (modifier: Modifier) -> Unit,
) {
  val darkBg = Color.Black
  val lightBg = Color.hex("#F5F5FA") ?: Color.Gray
  windowAdapterManager.provideRender(wid) { modifier ->
    WindowContentScaffoldWithTitleText(
      modifier = modifier,
      containerColor = when {
        LocalWindowControllerTheme.current.isDark -> darkBg
        else -> lightBg
      },
      topBarTitleText = AboutI18nResource.pageTitle(),
    ) { paddingValues ->
      render(Modifier.padding(paddingValues))
    }
  }
}

@Composable
fun AboutDetailsItem(modifier: Modifier = Modifier, labelName: String, text: String) {
  Row(
    modifier = modifier.fillMaxWidth().padding(8.dp).height(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelName,
      style = MaterialTheme.typography.labelMedium,
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun AboutDetailsListItem(modifier: Modifier = Modifier, labelName: String, textList: List<String>) {
  textList.forEachIndexed { index, text ->
    if (index == 0) {
      AboutDetailsItem(modifier = modifier, labelName = labelName, text = text)
    } else {
      AboutDetailsItem(modifier = modifier, labelName = "", text = text)
    }
  }
}

@Composable
fun AboutTitle(title: String) {
  Text(
    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    text = title,
    style = MaterialTheme.typography.labelMedium,
  )
}


@Composable
fun AboutContainer(content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
      color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
    ),
    content = content,
  )
}

@Composable
fun AboutHorizontalDivider() {
  Spacer(Modifier.height(8.dp))
}

data class AboutAppInfo(
  val appName: String = "Dweb Browser",
  val appVersion: String,
  val webviewVersion: String,
  val jmmVersion: Int = JsMicroModule.VERSION,
  val jmmPatch: Int = JsMicroModule.PATCH,
)

@Composable
fun AboutAppInfoRender(appInfo: AboutAppInfo) {
  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
      color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
    )
  ) {
    AboutDetailsItem(labelName = AboutI18nResource.appName.text, text = appInfo.appName)
    AboutDetailsItem(
      labelName = AboutI18nResource.appVersion.text, text = appInfo.appVersion
    )
    AboutDetailsItem(
      labelName = AboutI18nResource.webviewVersion.text, text = appInfo.webviewVersion
    )
    AboutDetailsItem(
      labelName = "JMM ${AboutI18nResource.version.text}", text = appInfo.jmmVersion.toString()
    )
    AboutDetailsItem(
      labelName = "JMM ${AboutI18nResource.patch.text}", text = appInfo.jmmPatch.toString()
    )
  }
}

@Composable
fun EnvSwitcherRender() {
  val changedKeys = remember { mutableListOf<ENV_SWITCH_KEY>() }
  AboutTitle(AboutI18nResource.experimental())
  if (changedKeys.isNotEmpty()) {
    Text(
      modifier = Modifier.padding(start = 24.dp, top = 8.dp),
      text = AboutI18nResource.experimentalChangedTip(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
    )
  }
  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
      color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
    )
  ) {
    for (switchKey in ENV_SWITCH_KEY.entries) {
      val experimental = switchKey.experimental ?: continue
      key(switchKey) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = experimental.title(),
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
              experimental.description(),
              modifier = Modifier.padding(start = 8.dp),
              style = MaterialTheme.typography.bodySmall
            )
          }
          val originEnabled = remember { envSwitch.isEnabled(switchKey) }
          var isEnabled by remember { mutableStateOf(originEnabled) }
          Switch(checked = isEnabled, onCheckedChange = {
            isEnabled = !isEnabled
            if (isEnabled != originEnabled) {
              changedKeys.add(switchKey)
            } else {
              changedKeys.remove(switchKey)
            }
            when {
              isEnabled -> envSwitch.enable(switchKey)
              else -> envSwitch.disable(switchKey)
            }
          })
        }
      }
    }

  }
}
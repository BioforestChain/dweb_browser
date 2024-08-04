package org.dweb_browser.browser.web.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.browser.web.model.LocalBrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserHomePage
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.sys.window.render.AppIconContainer
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BrowserHomePage.BrowserHomePageRender(modifier: Modifier = Modifier) {
  val viewModel = LocalBrowserViewModel.current
  val scope = rememberCoroutineScope()
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)
  ) {
    Column(
      Modifier.weight(1f, false).padding(top = 32.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
    ) {
      Image(
        painter = BrowserDrawResource.Logo.painter(),
        contentDescription = "Dweb Browser",
        modifier = Modifier.weight(1f).sizeIn(
          minWidth = 64.dp, minHeight = 64.dp, maxWidth = 280.dp, maxHeight = 280.dp
        ).fillMaxSize(),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
      )
      Text(
        text = "Dweb Browser", style = MaterialTheme.typography.titleMedium
      )
    }
    
    Row(
      Modifier.padding(horizontal = 16.dp).weight(1.618f).sizeIn(maxWidth = 480.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      browserPageList.forEach { pageType ->
        val onClick: () -> Unit = { scope.launch { viewModel.tryOpenUrlUI(pageType.url) } }
        Box(
          Modifier.clip(AppIconContainer.defaultShape).clickable(onClick = onClick).hoverCursor()
        ) {
          Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledIconButton(onClick, shape = AppIconContainer.defaultShape) {
              Icon(
                painter = pageType.iconPainter(),
                contentDescription = pageType.pageTitle(),
              )
            }
            Text(
              text = pageType.pageTitle(),
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun BrowserHomePagePreview() {
  val dnsNMM = DnsNMM()
  var browserHomePage by mutableStateOf<BrowserHomePage?>(null)
  LaunchedEffect(Unit) {
    val browserNMM = BrowserNMM()
    val fileNMM = FileNMM()
    dnsNMM.install(browserNMM)
    dnsNMM.install(fileNMM)
    val dnsRuntime = dnsNMM.bootstrap()
    val browserRuntime = dnsRuntime.open(browserNMM.mmid) as BrowserNMM.BrowserRuntime
    browserHomePage = BrowserHomePage(browserRuntime.browserController)
  }
  
  browserHomePage?.BrowserHomePageRender()
}
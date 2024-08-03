package org.dweb_browser.browser.data.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.browser.data.DataI18n
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface

@Composable
fun DataController.DetailRender() {
  val selectedProfileDetail by profileDetailFlow.collectAsState()
  when (val profileDetail = selectedProfileDetail) {
    null -> WindowContentRenderScope.Unspecified.WindowSurface {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(DataI18n.select_profile_for_detail_view())
      }
    }

    else -> WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
      Modifier.fillMaxSize(), topBarTitleText = profileDetail.short_name
    ) { paddingValues ->
      Box(Modifier.fillMaxSize().padding(paddingValues)) {
        NoDataRender(DataI18n.no_support_detail_view())
      }
    }
  }
}

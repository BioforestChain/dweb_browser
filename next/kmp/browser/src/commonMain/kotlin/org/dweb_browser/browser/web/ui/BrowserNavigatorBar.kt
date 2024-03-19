package org.dweb_browser.browser.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Filter1
import androidx.compose.material.icons.rounded.Filter2
import androidx.compose.material.icons.rounded.Filter3
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Filter5
import androidx.compose.material.icons.rounded.Filter6
import androidx.compose.material.icons.rounded.Filter7
import androidx.compose.material.icons.rounded.Filter8
import androidx.compose.material.icons.rounded.Filter9
import androidx.compose.material.icons.rounded.Filter9Plus
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.barcode.LocalQRCodeModel
import org.dweb_browser.browser.common.barcode.QRCodeState
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.model.page.BrowserWebPage

@Composable
fun BrowserNavigatorBar(viewModel: BrowserViewModel) {
  val uiScope = rememberCoroutineScope()
  val scope = viewModel.ioScope
  val qrCodeScanModel = LocalQRCodeModel.current
  val focusPage = viewModel.focusedPage ?: return
  key(focusPage) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(dimenNavigationHeight),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      val isWebPage = focusPage is BrowserWebPage

      BrowserNavigatorButton(
        imageVector = Icons.Rounded.AddHome,
        name = "AddHome",
        enabled = isWebPage
      ) {
        scope.launch { viewModel.addUrlToDesktopUI() }
      }
      if (isWebPage) {
        BrowserNavigatorButton(
          imageVector = Icons.Rounded.Add,
          name = "Add",
        ) {
          uiScope.launch {
            viewModel.addNewPageUI()
          }
        }
      } else {
        BrowserNavigatorButton(
          imageVector = Icons.Rounded.QrCodeScanner,
          name = "Scan",
        ) {
          qrCodeScanModel.updateQRCodeStateUI(QRCodeState.Scanning)
        }
      }
      BrowserNavigatorButton(
        imageVector = getMultiImageVector(viewModel.pageSize), // resId = R.drawable.ic_main_multi,
        name = "Open Preview Panel", enabled = true
      ) {
        viewModel.focusedPage?.captureViewInBackground()
        viewModel.toggleShowPreviewUI(true)
      }
      Box {
        BrowserMenuPanel()
        BrowserNavigatorButton(
          imageVector = Icons.Rounded.MoreVert, name = "Options", enabled = true
        ) {
          viewModel.showMore = true
        }
      }
    }
  }
}

internal fun getMultiImageVector(size: Int) = when (size) {
  1 -> Icons.Rounded.Filter1
  2 -> Icons.Rounded.Filter2
  3 -> Icons.Rounded.Filter3
  4 -> Icons.Rounded.Filter4
  5 -> Icons.Rounded.Filter5
  6 -> Icons.Rounded.Filter6
  7 -> Icons.Rounded.Filter7
  8 -> Icons.Rounded.Filter8
  9 -> Icons.Rounded.Filter9
  else -> Icons.Rounded.Filter9Plus
}

@Composable
private fun BrowserNavigatorButton(
  imageVector: ImageVector, name: String, enabled: Boolean = true, onClick: () -> Unit
) {
  IconButton(
    onClick = onClick,
    enabled = enabled
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = name,
      tint = if (enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
      } else {
        MaterialTheme.colorScheme.outlineVariant
      }
    )
  }
}
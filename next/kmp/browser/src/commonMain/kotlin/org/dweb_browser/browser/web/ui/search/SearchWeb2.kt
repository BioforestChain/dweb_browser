package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.ui.page.SearchEngineToggleButton
import org.dweb_browser.helper.format
import org.dweb_browser.helper.mapFindNoNull
import org.dweb_browser.helper.toWebUrl

@Composable
internal fun SearchWeb2(
  viewModel: BrowserViewModel,
  searchTextState: TextFieldState,
  onDismissRequest: () -> Unit,
) {
  val searchText = searchTextState.text.toString()
  LazyColumn(Modifier.fillMaxSize()) {
    val list = viewModel.filterAllEngines

    val urlAsKeyword = searchText.toWebUrl()?.let { url ->
      viewModel.filterAllEngines.mapFindNoNull { it.queryKeyWordValue(url) }
    }


    // 标题
    item {
      PanelTitle(
        BrowserI18nResource.browser_search_engine(),
        titleIcon = { Icon(Icons.Default.TravelExplore, "") },
      ) {
        urlAsKeyword?.also {
          InputChip(
            selected = false,
            onClick = {
              searchTextState.setTextAndPlaceCursorAtEnd(it)
            },
            label = { Text(it) },
            trailingIcon = { Icon(Icons.Rounded.Edit, "fill to search input") },
          )
        }

      }
    }

    val keyword = urlAsKeyword ?: searchText

    itemsIndexed(list) { index, searchEngine ->
      key(searchEngine.host) {
        if (index > 0) HorizontalDivider()
        val colors = ListItemDefaults.colors()
        ListItem(
          colors = colors,
          modifier = Modifier.clickable(enabled = searchEngine.enable) {
            viewModel.doIOSearchUrl(searchEngine.searchLinks.first().format(keyword))
            onDismissRequest()
          },
          leadingContent = {
            Image(
              painter = searchEngine.painter(),
              contentDescription = searchEngine.displayName,
              modifier = Modifier.size(36.dp),
            )
          },
          headlineContent = {
            if (searchEngine.enable) {
              Text(
                text = BrowserI18nResource.browser_search_keyword(keyword),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            } else {
              Text(
                text = BrowserI18nResource.browser_engine_inactive(),
                color = colors.disabledHeadlineColor
              )
            }
          },
          supportingContent = {
            Text(
              text = "${searchEngine.displayName} ${searchEngine.host}",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          trailingContent = {
            if (searchEngine.enable) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
              )
            } else {
              SearchEngineToggleButton(engineItem = searchEngine, viewModel)
            }
          },
        )
      }
    }

    val disableLocalSearch = true
    if (disableLocalSearch) {
      return@LazyColumn
    }
    val injectList = viewModel.searchInjectList
    /// 标题
    item {
      PanelTitle(
        BrowserI18nResource.browser_search_local_resources(),
        titleIcon = { Icon(Icons.Default.FindInPage, "") },
      )
    }

    if (injectList.isEmpty()) {
      item {
        ListItem(
          modifier = Modifier.fillMaxWidth(),
          headlineContent = {
            Text(text = BrowserI18nResource.browser_search_noFound())
          },
          leadingContent = {
            Icon(
              imageVector = Icons.Default.Error,
              contentDescription = null,
              modifier = Modifier.size(40.dp)
            )
          },
        )
      }
      return@LazyColumn
    }
    itemsIndexed(injectList) { index, searchInject ->
      if (index > 0) HorizontalDivider()
      ListItem(
        headlineContent = {
          Text(text = searchInject.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.clickable {
          // TODO
          onDismissRequest()
        },
        supportingContent = {
          Text(text = searchText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
          Image(searchInject.iconPainter(), contentDescription = searchInject.name)
        },
      )
    }
  }
}
package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.http.Url
import io.ktor.http.headers
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.ui.IconRender
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.commonConsumeEachArrayRange
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.isWebUrlOrWithoutProtocol
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.sha256
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import kotlin.coroutines.CoroutineContext
import kotlin.text.lowercase

@Composable
internal fun SearchWeb3(
  viewModel: BrowserViewModel,
  web3Searcher: Web3Searcher?,
  onDismissRequest: () -> Unit,
) {
  LaunchedEffect(web3Searcher) {
    web3Searcher?.doSearchDwebApps()
  }
  LazyColumn {
    val dwebappsEntries = (web3Searcher?.dwebappMap ?: emptyMap()).toList()
    item {
      PanelTitle(
        BrowserI18nResource.browser_search_dwebapp(),
        titleIcon = { Icon(Icons.Default.AppShortcut, "") },
        enabled = web3Searcher != null,
        trailingContent = {
          if (dwebappsEntries.isNotEmpty()) {
            Text(
              BrowserI18nResource.browser_web3_found_dwebapps(dwebappsEntries.size.toString()),
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.alpha(0.6f)
            )
          }
        }
      )
    }
    items(dwebappsEntries.size) {
      val (_, apps) = dwebappsEntries[it]
      val info = apps.first()
      val app = info.app
      val first = it == 0
      val last = it == dwebappsEntries.size - 1
      if (!first) {
        HorizontalDivider()
      }
      ListItem(
        modifier = Modifier.clickable {
          viewModel.browserNMM.scopeLaunch(cancelable = true) {
            viewModel.browserNMM.nativeFetch(
              PureClientRequest(
                "dweb://install?url=${info.manifestUrl.encodeURIComponent()}",
                PureMethod.GET,
                headers = PureHeaders(headers = headers {
                  "Referrer" to info.originUrl
                })
              )
            )
          }
        },
        leadingContent = {
          app.IconRender()
        },
        overlineContent = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(app.id)
            Text("v" + app.version)
          }
        },
        headlineContent = { Text(app.name) },
        supportingContent = {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(app.bundle_size.toSpaceSize())
            /// 一些认证信息的徽章
            // FilterChip()
            // SuggestionChip()
          }
        },
        trailingContent = {
          Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, "Go To Detail")
          }
        },
      )
      if (!last) {
        HorizontalDivider()
      }
    }
    item {
      LazyColumn(
        Modifier.fillMaxWidth().heightIn(max = 360.dp)
          .background(MaterialTheme.colorScheme.surfaceContainer)
      ) {
        val logs = web3Searcher?.logList ?: emptyList()
        items(logs.size) {
          val log = logs[logs.size - it - 1]
          val first = it == 0
          val last = it == logs.size - 1
          Text(
            log,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp).padding(
              top = if (first) 8.dp else 0.dp,
              bottom = if (last) 8.dp else 0.dp,
            )
          )
          if (!last) {
            HorizontalDivider()
          }

        }
      }
    }
  }
}

internal class Web3Searcher(
  override val coroutineContext: CoroutineContext,
  val searchText: String,
) : CoroutineScope {

  /**
   * 这是新语法，如果你的IDE报错：
   * In your IDE,
   * go to Settings | Languages & Frameworks | Kotlin and select the Enable K2 mode option.
   * The IDE will analyze your code using its K2 mode.
   */
//  val logList : List<String>
//  field = mutableStateListOf()
  val logList = mutableStateListOf<String>()

  private fun log(log: String) {
    logList.add(log)
  }

  val dwebappMap = mutableStateMapOf<String, MutableList<SearchedDwebAppInfo>>()
//  private fun addApp(app: JmmAppInstallManifest) {
//    dwebappList.add(app)
//  }

  private suspend fun tryParse(
    originHref: String,
    originUrl: Url,
    metadataHref: String,
    integrity: String?,
  ) {
    log("!!dwebapp $originHref parsing: $metadataHref")
    try {
      val metadataRes = httpFetch(
        PureClientRequest(
          href = metadataHref,
          method = PureMethod.GET,
          headers = PureHeaders(headers {
            "Accept" to "application/json"
            "Referer" to originHref
          }),
        )
      )
      /// 验证跨域
      metadataRes.headers.get("Access-Control-Allow-Origin")?.also { allowOrigin ->
        if (allowOrigin != "*" && allowOrigin != originUrl.protocolWithAuthority) {
          log("error: $originHref dwebapp cors limit")
        }
      }
      /// 完整性验证
      integrity?.lowercase()?.also { integrity ->
        if (integrity.startsWith("sha256-")) {
          val hash = integrity.substringAfter("sha256-")
          if (sha256(metadataRes.binary()).hexString != hash) {
            log("error: $originHref integrity hash not match")
          }
        }
      }

      /// 加入到列表中
      val manifest = metadataRes.json<JmmAppInstallManifest>()
      val apps = dwebappMap.getOrElse(manifest.id) { mutableStateListOf() }
      apps.add(SearchedDwebAppInfo(manifest, metadataHref, originHref))
      /// TODO 根据可信度、版本号进行排序
      //  apps.sortedWith {  }

      log("success $originHref found dwebapp: ${manifest.name}(${manifest.id})")
      dwebappMap[manifest.id] = apps
    } catch (e: Throwable) {
      log("error $originHref : ${e.message}")
    }
  }

  suspend fun doSearchDwebApps() {
    flow {
      if (searchText.isWebUrl()) {
        emit(searchText)
        return@flow
      }
      if (searchText.isWebUrlOrWithoutProtocol()) {
        emit("https://$searchText")
        emit("https://dweb.$searchText")
      }
      flow {
        emit("com")
        emit("org")
        emit("net")
      }.collect { top ->
        emit("https://$searchText.$top")
        emit("https://www.$searchText.$top")
        emit("https://dweb.$searchText.$top")
        emit("https://dweb-$searchText.$top")
        emit("https://$searchText-dweb.$top")
      }
    }.collect { originHref ->
      val originUrl = originHref.toWebUrl() ?: return@collect
      log("fetching $originHref")
      val res = httpFetch(originHref)
      if (!res.isOk || res.headers.get("Content-Type")?.contains("text/html") != true) {
        log("fail $originHref")
        return@collect
      }
      log("parsing $originHref")
      var headClose = false
      val htmlParser = KsoupHtmlParser(handler = object : KsoupHtmlHandler {
        override fun onCloseTag(name: String, isImplied: Boolean) {
          headClose = name == "head"
        }

        override fun onOpenTag(name: String, attributes: Map<String, String>, isImplied: Boolean) {
          if (name != "link") return
          log("link $originHref $attributes")
          if (attributes["rel"] != "dwebapp") return
          val metadataHref = attributes["href"] ?: return

          CoroutineScope(defaultAsyncExceptionHandler).launch {
            tryParse(originHref, originUrl, metadataHref, attributes["integrity"])
          }
        }
      })

      res.body.toPureStream().getReader("read html to parser")
        .commonConsumeEachArrayRange { chunk, last ->
          if (headClose) {
            this.breakLoop()
            return@commonConsumeEachArrayRange
          }
          htmlParser.write(chunk.utf8String)
          if (last) {
            htmlParser.end()
          }
        }
    }
  }
}

internal data class SearchedDwebAppInfo(
  val app: JmmAppInstallManifest,
  val manifestUrl: String,
  val originUrl: String,
)
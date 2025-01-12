package org.dweb_browser.browser.web.ui.search

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.http.Url
import io.ktor.http.headers
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.helper.Once
import org.dweb_browser.helper.commonConsumeEachArrayRange
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.isWebUrl
import org.dweb_browser.helper.isWebUrlOrWithoutProtocol
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.sha256
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import kotlin.coroutines.CoroutineContext

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
  val previewLog = mutableStateOf<String>("")

  private fun log(log: String) {
    logList.add(log)
    previewLog.value =
      BrowserI18nResource.Web3Search.preview_logs_lines.text(logList.size.toString())
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
    log(BrowserI18nResource.Web3Search.log_discover_dwebapps.text(originHref, metadataHref))
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
          log(BrowserI18nResource.Web3Search.log_error_cors_dwebapps.text(originHref))
        }
      }
      /// 完整性验证
      integrity?.lowercase()?.also { integrity ->
        if (integrity.startsWith("sha256-")) {
          val hash = integrity.substringAfter("sha256-")
          if (sha256(metadataRes.binary()).hexString != hash) {
            log(BrowserI18nResource.Web3Search.log_error_integrity_dwebapps.text(originHref))
          }
        }
      }

      /// 加入到列表中
      val manifest = metadataRes.json<JmmAppInstallManifest>()
      val apps = dwebappMap.getOrElse(manifest.id) { mutableStateListOf() }
      apps.add(SearchedDwebAppInfo(manifest, metadataHref, originHref))
      /// TODO 根据可信度、版本号进行排序
      //  apps.sortedWith {  }

      log(
        BrowserI18nResource.Web3Search.log_success_found_dwebapps.text(
          originHref, "${manifest.name}(${manifest.id})"
        )
      )
      dwebappMap[manifest.id] = apps
    } catch (e: Throwable) {
      log(
        BrowserI18nResource.Web3Search.log_error_dwebapps.text(
          originHref, e.message ?: "Unknown Reason"
        )
      )
    }
  }

  val doSearchDwebapps = Once {
    launch(coroutineContext) {
      log(BrowserI18nResource.Web3Search.log_start_dwebapps.text)
      // 创建一个 Semaphore 来限制并发数为 5
      val semaphore = Semaphore(5)
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
      }
        .collect { originHref ->
          launch {
            semaphore.withPermit {
              val originUrl = originHref.toWebUrl() ?: return@withPermit
              log(BrowserI18nResource.Web3Search.log_fetch_dwebapps.text(originHref))
              val res = httpFetch(originHref)
              if (!res.isOk || res.headers.get("Content-Type")?.contains("text/html") != true) {
                log(BrowserI18nResource.Web3Search.log_fail_dwebapps.text(originHref))
                return@withPermit
              }
              log(BrowserI18nResource.Web3Search.log_parse_dwebapps.text(originHref))
              var headClose = false
              val htmlParser = KsoupHtmlParser(handler = object : KsoupHtmlHandler {
                override fun onCloseTag(name: String, isImplied: Boolean) {
                  headClose = name == "head"
                }

                override fun onOpenTag(
                  name: String,
                  attributes: Map<String, String>,
                  isImplied: Boolean,
                ) {
                  if (name != "link") return
                  log(
                    "$originHref <$name ${
                      attributes.map { "${it.key}=\"${it.value}\"" }.joinToString(" ")
                    }/>"
                  )
                  if (attributes["rel"] != "dwebapp") return
                  val metadataHref = attributes["href"] ?: return

                  launch {
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
    }.invokeOnCompletion {
      log(BrowserI18nResource.Web3Search.log_end.text)
    }
  }
}

internal data class SearchedDwebAppInfo(
  val app: JmmAppInstallManifest,
  val manifestUrl: String,
  val originUrl: String,
)
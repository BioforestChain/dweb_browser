package org.dweb_browser.common.webview

public sealed class WebContent {
  public data class Url(
    val url: String,
    val additionalHttpHeaders: Map<String, String> = emptyMap(),
  ) : WebContent()

  public data class Data(
    val data: String,
    val baseUrl: String? = null,
    val encoding: String = "utf-8",
    val mimeType: String? = null,
    val historyUrl: String? = null,
  ) : WebContent()

  public data class Post(
    val url: String,
    val postData: ByteArray,
  ) : WebContent() {

    override fun hashCode(): Int {
      var result = url.hashCode()
      result = 31 * result + postData.contentHashCode()
      return result
    }
  }

  public object NavigatorOnly : WebContent()
}

internal fun WebContent.withUrl(url: String) = when (this) {
  is WebContent.Url -> copy(url = url)
  else -> WebContent.Url(url)
}

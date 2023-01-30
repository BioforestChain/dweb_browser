package info.bagen.rust.plaoc.webkit

sealed class AdWebContent {
    data class Url(
        val url: String,
        val additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) : AdWebContent()

    data class Data(val data: String, val baseUrl: String? = null) : AdWebContent()

    fun getCurrentUrl(): String? {
        return when (this) {
            is Url -> url
            is Data -> baseUrl
            else -> null
        }
    }
}

internal fun AdWebContent.withUrl(url: String) = when (this) {
    is AdWebContent.Url -> copy(url = url)
    else -> AdWebContent.Url(url)
}

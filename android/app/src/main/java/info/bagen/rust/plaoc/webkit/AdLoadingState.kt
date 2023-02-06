package info.bagen.rust.plaoc.webkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import info.bagen.rust.plaoc.webkit.AdLoadingState.Finished
import info.bagen.rust.plaoc.webkit.AdLoadingState.Loading

/**
 * Sealed class for constraining possible loading states.
 * See [Loading] and [Finished].
 */
sealed class AdLoadingState {
    /**
     * Describes a WebView that has not yet loaded for the first time.
     */
    object Initializing : AdLoadingState()

    /**
     * Describes a webview between `onPageStarted` and `onPageFinished` events, contains a
     * [progress] property which is updated by the webview.
     */
    data class Loading(val progress: Float) : AdLoadingState()

    /**
     * Describes a webview that has finished loading content.
     */
    object Finished : AdLoadingState()
}

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param url The url to load in the WebView
 * @param additionalHttpHeaders Optional, additional HTTP headers that are passed to [AdWebView.loadUrl].
 *                              Note that these headers are used for all subsequent requests of the WebView.
 */
@Composable
fun rememberAdWebViewState(url: String, additionalHttpHeaders: Map<String, String> = emptyMap()) =
    remember(url, additionalHttpHeaders) {
        AdWebViewState(
            AdWebContent.Url(
                url = url,
                additionalHttpHeaders = additionalHttpHeaders
            )
        )
    }

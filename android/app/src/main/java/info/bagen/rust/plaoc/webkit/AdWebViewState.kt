package info.bagen.rust.plaoc.webkit

import android.graphics.Bitmap
import androidx.compose.runtime.*

/**
 * A state holder to hold the state for the WebView. In most cases this will be remembered
 * using the rememberWebViewState(uri) function.
 */
@Stable
class AdWebViewState(webContent: AdWebContent) {
    /**
     *  The content being loaded by the WebView
     */
    var content by mutableStateOf<AdWebContent>(webContent)

    /**
     * Whether the WebView is currently [AdLoadingState.Loading] data in its main frame (along with
     * progress) or the data loading has [AdLoadingState.Finished]. See [AdLoadingState]
     */
    var loadingState: AdLoadingState by mutableStateOf(AdLoadingState.Initializing)
        internal set

    /**
     * Whether the webview is currently loading data in its main frame
     */
    val isLoading: Boolean
        get() = loadingState !is AdLoadingState.Finished

    /**
     * The title received from the loaded content of the current page
     */
    var pageTitle: String? by mutableStateOf(null)
        internal set

    /**
     * the favicon received from the loaded content of the current page
     */
    var pageIcon: Bitmap? by mutableStateOf(null)
        internal set

    /**
     * A list for errors captured in the last load. Reset when a new page is loaded.
     * Errors could be from any resource (iframe, image, etc.), not just for the main page.
     * For more fine grained control use the OnError callback of the WebView.
     */
    val errorsForCurrentRequest = mutableStateListOf<AdWebViewError>()
}

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param data The uri to load in the WebView
 */
@Composable
fun rememberAdWebViewStateWithHTMLData(data: String, baseUrl: String? = null) =
    remember(data, baseUrl) { AdWebViewState(AdWebContent.Data(data, baseUrl)) }

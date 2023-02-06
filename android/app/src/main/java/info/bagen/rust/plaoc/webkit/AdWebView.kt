/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.bagen.rust.plaoc.webkit

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.rust.plaoc.webkit.inputFile.AdFileInputHelper
import info.bagen.rust.plaoc.webkit.inputFile.rememberAdFileInputHelper

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * If you require more customisation you are most likely better rolling your own and using this
 * wrapper as an example.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param captureBackPresses Set to true to have this Composable capture back presses and navigate
 * the WebView back.
 * @param navigator An optional navigator object that can be used to control the WebView's
 * navigation from outside the composable.
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 * @sample com.google.accompanist.sample.webview.BasicWebViewSample
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdWebView(
    state: AdWebViewState,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    navigator: AdWebViewNavigator = rememberAdWebViewNavigator(),
    fileInputHelper: AdFileInputHelper = rememberAdFileInputHelper(),
    onCreated: (AdAndroidWebView) -> Unit = {},
    client: AdWebViewClient = remember { AdWebViewClient() },
    chromeClient: AdWebChromeClient = remember { AdWebChromeClient() }
) {
    var webView by remember { mutableStateOf<AdAndroidWebView?>(null) }

    BackHandler(captureBackPresses && navigator.canGoBack) {
        webView?.goBack()
    }

    LaunchedEffect(webView, navigator) {
        with(navigator) { webView?.handleNavigationEvents() }
    }

    DisposableEffect(
        AndroidView(
            factory = { context ->
                AdAndroidWebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.setSupportMultipleWindows(true)
                    settings.allowFileAccess = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.safeBrowsingEnabled = true
                    settings.setGeolocationEnabled(true)
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true

                    onCreated(this)

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Set the state of the client and chrome client
                    // This is done internally to ensure they always are the same instance as the
                    // parent Web composable
                    client.state = state
                    client.navigator = navigator
                    chromeClient.state = state
                    chromeClient.fileInputHelper = fileInputHelper

                    webChromeClient = chromeClient
                    webViewClient = client
                }.also { webView = it }
            },
            modifier = modifier
        ) { view ->
            when (val content = state.content) {
                is AdWebContent.Url -> {
                    val url = content.url

                    if (url.isNotEmpty() && url != view.url) {
                        view.loadUrl(url, content.additionalHttpHeaders.toMutableMap())
                    }
                }
                is AdWebContent.Data -> {
                    view.loadDataWithBaseURL(content.baseUrl, content.data, null, "utf-8", null)
                }
            }

            navigator.canGoBack = view.canGoBack()
            navigator.canGoForward = view.canGoForward()
        }
    ) {
        onDispose {
            webView?.let { it.destroy() } // 为了修复播放音频后返回时，声音仍然在播放的问题
        }
    }
}

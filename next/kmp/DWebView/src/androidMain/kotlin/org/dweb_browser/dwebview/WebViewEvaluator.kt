package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.dweb_browser.helper.*
import java.util.concurrent.atomic.AtomicInteger

typealias AsyncChannel = Channel<Result<String>>;


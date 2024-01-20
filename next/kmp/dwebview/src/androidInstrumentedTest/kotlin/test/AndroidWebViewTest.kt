package test


import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AndroidWebViewTest {
  @Test
  fun canGoBack() = runBlocking(Dispatchers.Main) {
    val appContext = ApplicationProvider.getApplicationContext<Context>()

    val webview = WebView(appContext)
    /// 只能在主现场调用
    println("canGoBack in Main=${webview.canGoBack()}")
  }
}

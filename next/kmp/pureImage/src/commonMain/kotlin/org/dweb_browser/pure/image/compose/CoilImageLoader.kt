package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.content.OutgoingContent
import io.ktor.util.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenEntries
import kotlinx.coroutines.CoroutineDispatcher
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHookContext
import org.dweb_browser.pure.image.removeOriginAndAcceptEncoding
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

val debugCoilImageLoader = Debugger("coilImageLoader")

val LocalCoilImageLoader = compositionLocalOf { CoilImageLoader(null) }

class CoilImageLoader(private val diskCache: DiskCache? = null) {
  private val hooks = mutableSetOf<FetchHook>()
  private var cache: Pair<PlatformContext, ImageLoader>? = null
  val currentLoader: ImageLoader
    @Composable
    get() {
      val cache = cache
      if (LocalPlatformContext.current == cache?.first) {
        return cache.second
      }
      val platformContext = LocalPlatformContext.current
      val scope = rememberCoroutineScope()
      return buildLoader(scope.coroutineContext, platformContext, hooks, diskCache).also {
        this.cache = platformContext to it
      }
    }

  @Composable
  fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook? = null
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return Load(url, containerWidth, containerHeight, hook)
  }

  @OptIn(ExperimentalCoilApi::class)
  @Composable
  fun Load(
    url: String, containerWidth: Int, containerHeight: Int, hook: FetchHook? = null
  ): ImageLoadResult {
    val requestHref = url.replace(Regex("\\{WIDTH\\}"), containerWidth.toString())
      .replace(Regex("\\{HEIGHT\\}"), containerHeight.toString())
    val safeHook: FetchHook? = remember(requestHref, hook) {
      hook?.let {
        {
          when (request.href) {
            requestHref -> hook()
            else -> null
          }
        }
      }
    }

    if (safeHook != null) {
      hooks.add(safeHook)
    }

    val loader = currentLoader

    val platformContext = LocalPlatformContext.current
    return produceState(ImageLoadResult.Setup) {
      value = ImageLoadResult.Loading;
      val imgReq = ImageRequest.Builder(platformContext).run {
        size(containerWidth, containerHeight)
        data(url)
        build()
      }
      value = when (val result = loader.execute(imgReq)) {
        is ErrorResult -> ImageLoadResult.error(result.throwable)
        is SuccessResult -> ImageLoadResult.success(
          result.image.toImageBitmap()
        )
      }
      if (safeHook != null) {
        hooks.remove(safeHook)
      }
    }.value
  }

  companion object {

    private val defaultHttpClient = lazy {
      @Suppress("USELESS_IS_CHECK")
      when (val pureClient = defaultHttpPureClient) {
        is KtorPureClient -> pureClient.ktorClient
        else -> HttpClient()
      }
    }

    private fun buildLoader(
      coroutineContext: CoroutineContext,
      platformContext: PlatformContext,
      hooks: Set<FetchHook>? = null,
      diskCache: DiskCache? = null
    ): ImageLoader = ImageLoader.Builder(platformContext).components {
      if (hooks == null) {
        add(KtorNetworkFetcherFactory(defaultHttpClient))
      } else {
        add(KtorNetworkFetcherFactory(lazy {
          val ktorClient = defaultHttpClient.value
          val ktorEngine = ktorClient.engine
          HttpClient(engine = object : HttpClientEngine {
            @InternalAPI
            override suspend fun execute(data: HttpRequestData) = measureTimedValue {
              val hookList = hooks.toList()
              val hookContext by lazy {
                FetchHookContext(
                  PureServerRequest(
                    data.url.toString(),
                    PureMethod.from(data.method),
                    PureHeaders(
                      data.headers.flattenEntries().removeOriginAndAcceptEncoding()
                    ),
                    when (val body = data.body) {
                      is OutgoingContent.ByteArrayContent -> IPureBody.from(body.bytes())
                      is OutgoingContent.NoContent -> IPureBody.Empty
                      is OutgoingContent.ProtocolUpgrade -> throw Exception("no support ProtocolUpgrade")
                      is OutgoingContent.ReadChannelContent -> IPureBody.from(PureStream(body.readFrom()))
                      is OutgoingContent.WriteChannelContent -> throw Exception("no support WriteChannelContent")
                    }
                  ),
                )
              }
              for (hook in hookList) {
                val pureResponse = hookContext.hook() ?: continue
                return@measureTimedValue HttpResponseData(
                  statusCode = pureResponse.status,
                  headers = Headers.build {
                    for ((key, value) in pureResponse.headers) {
                      this.append(key, value)
                    }
                  },
                  body = pureResponse.body.toPureStream().getReader("to HttpResponseData"),
                  version = HttpProtocolVersion.HTTP_1_1,
                  requestTime = GMTDate(null),
                  callContext = ioAsyncExceptionHandler
                )
              }
              ktorEngine.execute(data)
            }.let {
              debugCoilImageLoader("execute", "url=${data.url} duration=${it.duration}")
              it.value
            }

            override val config: HttpClientEngineConfig = ktorEngine.config
            override val dispatcher: CoroutineDispatcher = ktorEngine.dispatcher

            override fun close() {
              ktorEngine.close()
            }

            override val coroutineContext: CoroutineContext = ktorEngine.coroutineContext
          }) { }
        }))
      }
      add(SvgDecoder.Factory())
    }.memoryCache {
      MemoryCache.Builder()
        // Set the max size to 25% of the app's available memory.
        .maxSizePercent(platformContext, percent = 0.25)
        .build()
    }.diskCache(diskCache)
      // Show a short crossfade when loading images asynchronously.
      .crossfade(true)
      .build()
  }
}


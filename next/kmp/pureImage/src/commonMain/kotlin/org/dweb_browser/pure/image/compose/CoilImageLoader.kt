package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.ComponentRegistry
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
import io.ktor.client.engine.callContext
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
import org.dweb_browser.helper.buildUrlString
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

class CoilImageLoader(private val diskCache: DiskCache? = null) : PureImageLoader {
  private val hooks = mutableSetOf<FetchHook>()
  private var cache: Pair<PlatformContext, ImageLoader>? = null
  private fun getLoader(platformContext: PlatformContext): ImageLoader {
    val cache = cache
    if (platformContext == cache?.first) {
      return cache.second
    }
    return buildLoader(platformContext, hooks, diskCache).also {
      this.cache = platformContext to it
    }
  }

  @Composable
  override fun Load(
    url: String, maxWidth: Dp, maxHeight: Dp, hook: FetchHook?
  ): ImageLoadResult {
    val density = LocalDensity.current.density
    // 这里直接计算应该会比remember来的快
    val containerWidth = (maxWidth.value * density).toInt()
    val containerHeight = (maxHeight.value * density).toInt()
    return Load(url, containerWidth, containerHeight, hook)
  }

  @OptIn(ExperimentalCoilApi::class)
  @Composable
  fun Load(
    url: String, containerWidth: Int, containerHeight: Int, hook: FetchHook? = null
  ): ImageLoadResult {
    val requestHref = remember(url) {
      url.replace(Regex("\\{WIDTH\\}"), containerWidth.toString())
        .replace(Regex("\\{HEIGHT\\}"), containerHeight.toString())
    }
    /// 这里需要对url进行一次统一的包装，以避免coil的keyer面对file协议的时候异常
    val wrappedRequestHref = remember(requestHref) {
      buildUrlString("https://image.std.dweb") {
        parameters.append("url", requestHref)
      }
    }
    val safeHook: FetchHook? = remember(wrappedRequestHref, hook) {
      hook?.let {
        {
          when (request.href) {
            wrappedRequestHref -> this.copy(request = request.copy(href = requestHref)).hook()
            else -> null
          }
        }
      }
    }

    if (safeHook != null) {
      hooks.add(safeHook)
    }

    val platformContext = LocalPlatformContext.current
    val loader = getLoader(platformContext)

    return produceState(ImageLoadResult.Setup) {
      value = ImageLoadResult.Loading;
      val imgReq = ImageRequest.Builder(platformContext).run {
        size(containerWidth, containerHeight)
        data(wrappedRequestHref)
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

    private val defaultHttpClient = @Suppress("USELESS_IS_CHECK")
    when (val pureClient = defaultHttpPureClient) {
      is KtorPureClient -> pureClient.ktorClient
      else -> HttpClient()
    }

    private fun buildLoader(
      platformContext: PlatformContext,
      hooks: Set<FetchHook>? = null,
      diskCache: DiskCache? = null
    ): ImageLoader = ImageLoader.Builder(platformContext).components {
      if (hooks == null) {
        add(KtorNetworkFetcherFactory(defaultHttpClient))
      } else {
        val ktorEngine = defaultHttpClient.engine
        add(
          KtorNetworkFetcherFactory(
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
                        append(key, value)
                      }
                    },
                    /// 这里硬性要求返回 ByteReadChannel
                    body = pureResponse.stream().getReader("to HttpResponseData"),
                    version = HttpProtocolVersion.HTTP_1_1,
                    requestTime = GMTDate(null),
                    callContext = callContext()
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
            })
          )
        )
      }
      add(SvgDecoder.Factory())
      addPlatformComponents()
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

internal expect fun ComponentRegistry.Builder.addPlatformComponents()

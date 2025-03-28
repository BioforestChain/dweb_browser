package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
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
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenEntries
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.ext.FetchHook
import org.dweb_browser.pure.http.ext.FetchHookContext
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toPureBody
import org.dweb_browser.pure.image.removeOriginAndAcceptEncoding
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

val debugCoilImageLoader = Debugger("coilImageLoader")
val LocalCoilImageLoader = compositionLocalOf { CoilImageLoader.defaultInstance }

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

  private val scope = globalDefaultScope
  private val sharedLoaderResults = LoaderCacheMap<MutableStateFlow<ImageLoadResult>>(scope)

  @Composable
  override fun Load(
    task: LoaderTask,
  ): ImageLoadResult {
    val platformContext = LocalPlatformContext.current
    val imageLoader = this@CoilImageLoader.ImageLoader(platformContext)
    val taskLoader = remember(task.key, imageLoader, platformContext) {
      startLoad(platformContext, imageLoader, task)
    }
    return taskLoader.result.collectAsState().value
  }

  @Composable
  fun ImageLoader(platformContext: PlatformContext = LocalPlatformContext.current): ImageLoader {
    return remember(platformContext) { getLoader(platformContext) }
  }

  inner class TaskLoader(
    val task: LoaderTask,
    val context: PlatformContext,
    val loader: ImageLoader,
    val result: MutableStateFlow<ImageLoadResult>,
  ) {
    init {
      val imageResultState = this.result

      scope.launch {
        val requestHref = task.url.replace("{WIDTH}", task.containerWidth.toString())
          .replace("{HEIGHT}", task.containerHeight.toString())
        /// 这里需要对url进行一次统一的包装，以避免coil的keyer面对file协议的时候异常
        val wrappedRequestHref = when {
          requestHref.startsWith("http") -> requestHref
          else -> buildUrlString("https://image.std.dweb") {
            parameters.append("url", requestHref)
          }
        }
        val safeHook: FetchHook? = task.hook?.let { hook ->
          {
            when (request.href) {
              wrappedRequestHref -> this.copy(request = request.copy(href = requestHref)).hook()
              else -> null
            }
          }
        }
        if (safeHook != null) {
          hooks.add(safeHook)
        }
        imageResultState.value = ImageLoadResult.Loading;
        val imgReq = ImageRequest.Builder(context).run {
          size(task.containerWidth, task.containerHeight)
          data(wrappedRequestHref)
          build()
        }
        imageResultState.value = when (val result = loader.execute(imgReq)) {
          is ErrorResult -> ImageLoadResult.error(result.throwable).also { res ->
            val failTimes = PureImageLoader.urlErrorCount.getOrPut(task.url) { 0 } + 1
            PureImageLoader.urlErrorCount[task.url] = failTimes
          }

          is SuccessResult -> {
            PureImageLoader.urlErrorCount.remove(task.url)
            ImageLoadResult.success(result.image.toImageBitmap(), imgReq, result)
          }
        }

        if (safeHook != null) {
          hooks.remove(safeHook)
        }
      }
      imageResultState
    }
  }


  fun startLoad(
    context: PlatformContext,
    loader: ImageLoader,
    task: LoaderTask,
  ): TaskLoader {
    return TaskLoader(
      task,
      context,
      loader,
      sharedLoaderResults.get(task) ?: MutableStateFlow(ImageLoadResult.Setup)
    ).also { loader ->
      val cacheItem = CacheItem(task, loader.result)
      sharedLoaderResults.save(cacheItem)
      ResvgImageLoader.Companion.scope.launch {
        loader.result.collect { result ->
          if (result.isError) {
            /// 失败后，移除执行缓存。但是这里的result仍然不会变
            if (cacheItem.result.value == loader.result) {
              sharedLoaderResults.delete(task, cacheItem)
            }
          }
        }
      }
    }
  }

  companion object {
    val defaultInstance by lazy { CoilImageLoader(null) }
    private val defaultHttpClient =
      @Suppress("USELESS_IS_CHECK") when (val pureClient = defaultHttpPureClient) {
        is KtorPureClient<*> -> pureClient.ktorClient
        else -> HttpClient()
      }

    private fun buildLoader(
      platformContext: PlatformContext,
      hooks: Set<FetchHook>? = null,
      diskCache: DiskCache? = null,
    ): ImageLoader = SingletonImageLoader.get(platformContext).let { defaultLoader ->
      defaultLoader.newBuilder().components(
        defaultLoader.components.newBuilder().apply {
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
                          data.url.toString(), PureMethod.from(data.method), PureHeaders(
                            data.headers.flattenEntries().removeOriginAndAcceptEncoding()
                          ), data.body.toPureBody()
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
                  }.value

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
        }.build()
      ).memoryCache {
        MemoryCache.Builder()
          // Set the max size to 25% of the app's available memory.
          .maxSizePercent(platformContext, percent = 0.25).build()
      }.diskCache(diskCache)
        // Show a short crossfade when loading images asynchronously.
        .crossfade(true).build()
    }
  }
}

internal expect fun ComponentRegistry.Builder.addPlatformComponents()

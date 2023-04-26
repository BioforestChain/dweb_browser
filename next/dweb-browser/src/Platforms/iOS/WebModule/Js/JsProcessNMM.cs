using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http.Net;
using DwebBrowser.Helper;
using System.Text.Json;
using WebKit;
using System.Diagnostics;

#nullable enable

namespace DwebBrowser.WebModule.Js;

public class JsProcessNMM : NativeMicroModule
{
    public JsProcessNMM() : base("js.sys.dweb")
    {
        _LAZY_JS_PROCESS_WORKER_CODE = new Lazy<string>(() =>
            Task.Run(async () => await (await NativeFetchAsync("file:///bundle/js-process.worker.js")).TextAsync()).Result);
    }

    private Lazy<string> _LAZY_JS_PROCESS_WORKER_CODE;
    private string _JS_PROCESS_WORKER_CODE
    {
        get => _LAZY_JS_PROCESS_WORKER_CODE.Value;
    }

    private Dictionary<string, string> _CORS_HEADERS = new()
    {
        { "Content-Type", "text/javascript" },
        { "Access-Control-Allow-Origin", "*" },
        { "Access-Control-Allow-Headers", "*" }, // 要支持 X-Dweb-Host
        { "Access-Control-Allow-Methods", "*" }
    };

    private static string s_INTERNAL_PATH_RAW = "/<internal>";
    private static string s_INTERNAL_PATH = s_INTERNAL_PATH_RAW.EncodeURI();

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        // 将本地资源文件读取添加到适配器中
        var cb = NativeFetch.NativeFetchAdaptersManager.Append(async (mm, request) =>
        {
            return LocaleFile.LocaleFileFetch(mm, request);
        });
        _onAfterShutdown += async (_) => { cb(); };

        /// 主页的网页服务
        var mainServer = await (await CreateHttpDwebServer(new DwebHttpServerOptions())).AlsoAsync(async server =>
        {
            // 在模块关停的时候，要关闭端口监听
            _onAfterShutdown += async (_) => { await server.Close(); };
            // 提供基本的主页服务
            var serverIpc = await server.Listen();
            serverIpc.OnRequest += async (request, ipc, _) =>
            {
                // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
                if (request.Uri.AbsolutePath.StartsWith(s_INTERNAL_PATH))
                {
                    var internalUri = request.Uri.Path(request.Uri.AbsolutePath.Substring(s_INTERNAL_PATH.Length));

                    if (internalUri.AbsolutePath == "/bootstrap.js")
                    {
                        await ipc.PostMessageAsync(
                            IpcResponse.FromText(
                                request.ReqId,
                                200,
                                /// 加入跨域支持
                                IpcHeaders.With(_CORS_HEADERS),
                                _JS_PROCESS_WORKER_CODE,
                                ipc));
                    }
                    else
                    {
                        await ipc.PostMessageAsync(
                            IpcResponse.FromText(
                                request.ReqId,
                                404,
                                /// 加入跨域支持
                                IpcHeaders.With(_CORS_HEADERS),
                                String.Format("// no found {0}", internalUri.AbsolutePath),
                                ipc));
                    }
                }
                else
                {
                    var response = await NativeFetchAsync(String.Format("file:///bundle/js-process{0}", request.Uri.AbsolutePath));
                    /// 加入跨域支持
                    foreach (var (key, value) in _CORS_HEADERS)
                    {
                        if (key is "Content-Type")
                        {
                            continue;
                        }
                        else if (!response.Headers.Contains(key))
                        {
                            response.Headers.TryAddWithoutValidation(key, value);
                        }
                    }

                    var res = await IpcResponse.FromResponse(request.ReqId, response, ipc);

                    await ipc.PostMessageAsync(res);
                }
            };
        });

        var bootstrap_url = mainServer.StartResult.urlInfo.BuildPublicDwebHref(
            mainServer.StartResult.urlInfo.BuildInternalUrl().Path(String.Format("{0}/bootstrap.js", s_INTERNAL_PATH))
        );

        var apis = await _createJsProcessWeb(mainServer);

        var ipcProcessIdMap = new Dictionary<Ipc, Dictionary<string, PromiseOut<int>>>();
        var processIpcMap = new Dictionary<string, Ipc>();
        //var ipcProcessIdMapLock = new Mutex();

        /// 创建 web worker
        /// request 需要携带一个流，来为 web worker 提供代码服务
        HttpRouter.AddRoute(HttpMethod.Post.Method, "/create-process", async (request, ipc) =>
        {
            processIpcMap.Add(ipc.Remote.Mmid, ipc);
            PromiseOut<int> po = null!;

            var processId = request.QueryValidate<string>("process_id")!;
            lock (ipcProcessIdMap)
            {
                var processIdMap = ipcProcessIdMap.GetValueOrPut(ipc, () =>
                {
                    ipc.OnClose += async (_) => { ipcProcessIdMap.Remove(ipc); };
                    return new Dictionary<string, PromiseOut<int>>();
                });

                if (processIdMap.Keys.Contains(processId))
                {
                    throw new Exception(String.Format("ipc:{0}/processId:{1} has already using", ipc.Remote.Mmid, processId));
                }

                po = new PromiseOut<int>().Also(it => processIdMap.Add(processId, it));
            }

            var result = await _createProcessAndRun(
                ipc,
                apis,
                bootstrap_url,
                request,
                request.QueryValidate<string>("entry", false));

            // 将自定义的 processId 与 真实的 js-process_id 进行关联
            po.Resolve(result.processHandler.Info.ProcessId);

            // 返回流，因为构建了一个双工通讯用于代码提供服务
            return result.streamIpc.ReadableStream.Stream;
        });

        /// 创建 web 通讯管道
        HttpRouter.AddRoute(HttpMethod.Get.Method, "/create-ipc", async (request, ipc) =>
        {
            var processId = request.QueryValidate<string>("process_id")!;

            /**
             * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
             * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
             */
            var mmid = request.QueryValidate<string>("mmid");

            int process_id;
            if (!ipcProcessIdMap.TryGetValue(ipc, out var processIdMap) || !processIdMap.TryGetValue(processId, out var po))
            {
                throw new Exception(String.Format("ipc:{0}/processId:{1} invalid", ipc.Remote.Mmid, processId));
            }
            process_id = await po.WaitPromiseAsync();

            // 返回 port_id
            return await _createIpc(ipc, apis, process_id, mmid);
        });

        /// 关闭 process
        HttpRouter.AddRoute(HttpMethod.Get.Method, "/close-process", async (request, ipc) =>
        {
            await CloseHttpDwebServer(new DwebHttpServerOptions(80, ipc.Remote.Mmid));
            var processIpc = processIpcMap.GetValueOrDefault(ipc.Remote.Mmid);
            processIpc?.Close();
            return true;
        });
    }

    private async Task<JsProcessWebApi> _createJsProcessWeb(HttpDwebServer mainServer)
    {
        var afterReadyPo = new PromiseOut<JsProcessWebApi>();
        /// WebView 实例
        var urlInfo = mainServer.StartResult.urlInfo;


        await MainQueue.Run(() =>
        {

            var dwebview = new DWebView.DWebView(localeMM: this);

            var apis = new JsProcessWebApi(dwebview).Also(apis =>
            {
                _onAfterShutdown += async (_) => { apis.Destroy(); };
            });
            dwebview.OnReady += async (_) =>
               afterReadyPo.Resolve(apis);
            var mainUrl = urlInfo.BuildPublicDwebHref(urlInfo.BuildInternalUrl().Path("/index.html"));
            dwebview.LoadURL(mainUrl);
        });
        var apis = await afterReadyPo.WaitPromiseAsync();
        return apis;

    }

    private async Task<CreateProcessAndRunResult> _createProcessAndRun(
        Ipc ipc,
        JsProcessWebApi apis,
        string bootstrap_url,
        HttpRequestMessage requestMessage,
        string? entry)
    {
        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        var httpDwebServer = await CreateHttpDwebServer(new DwebHttpServerOptions(subdomain: ipc.Remote.Mmid));

        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        var streamIpc = await new ReadableStreamIpc(ipc.Remote, "code-proxy-server").AlsoAsync(async it =>
        {
            it.BindIncomeStream(await requestMessage.Content.ReadAsStreamAsync());
        });

        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        var codeProxyServerIpc = await httpDwebServer.Listen();

        codeProxyServerIpc.OnRequest += async (request, ipc, _) =>
        {
            await ipc.PostResponseAsync(
                request.ReqId,
                // 转发给远端来处理
                // TODO：对代码进行翻译处理
                (await streamIpc.Request(request.ToRequest())).Let(it =>
                {
                    /// 加入跨域配置
                    var response = it;
                    foreach (var (key,value) in _CORS_HEADERS)
                    {
                        if (key is "Content-Type")
                        {
                            continue;
                        }
                        response.Headers.Add(key, value);
                    }

                    return response;
                }));
        };

        /// TODO: 需要传过来，而不是自己构建
        var metadata = new _JsProcessMetadata(ipc.Remote.Mmid);

        /// TODO: env 允许远端传过来扩展
        var env = new Dictionary<string, string>()
        {
            { "host", httpDwebServer.StartResult.urlInfo.Host },
            { "debug", "true" },
            { "ipc-support-protocols", "" }
        };

        /**
         * 创建一个通往 worker 的消息通道
         */
        var processHandler = await apis.CreateProcess(
            bootstrap_url,
            JsonSerializer.Serialize(metadata),
            JsonSerializer.Serialize(env),
            ipc.Remote,
            httpDwebServer.StartResult.urlInfo.Host);

        /**
         * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
         *
         * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
         */
        processHandler.Ipc.OnMessage += async (workerIpcMessage, _, _) =>
        {
            /**
             * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
             */
            await ipc.PostMessageAsync(workerIpcMessage);
        };
        ipc.OnMessage += async (remoteIpcMessage, _, _) =>
        {
            await processHandler.Ipc.PostMessageAsync(remoteIpcMessage);
        };

        /**
         * 开始执行代码
         */
        await apis.RunProcessMain(
            processHandler.Info.ProcessId,
            new JsProcessWebApi.RunProcessMainOptions(
                httpDwebServer.StartResult.urlInfo.BuildPublicDwebHref(
                    httpDwebServer.StartResult.urlInfo.BuildInternalUrl().Path(entry ?? "/index.js"))
                )
            );

        /// 绑定销毁
        /**
         * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
         *
         * > 自己shutdown的时候，这些ipc会被关闭
         */
        ipc.OnClose += async (_) =>
        {
            await httpDwebServer.Close();
            await apis.DestroyProcess(processHandler.Info.ProcessId);
        };

        return new CreateProcessAndRunResult(streamIpc, processHandler);
    }

    private record _JsProcessMetadata(Mmid mmid);

    public record CreateProcessAndRunResult(ReadableStreamIpc streamIpc, JsProcessWebApi.ProcessHandler processHandler);

    private Task<int> _createIpc(Ipc ipc, JsProcessWebApi apis, int process_id, Mmid mmid) =>
        apis.CreateIpc(process_id, mmid);
}


public static class MainQueue
{
    static IDispatcher? mainDispatcher = default;

    public static void Init()
    {
        Init(Dispatcher.GetForCurrentThread());
    }

    public static void Init(IDispatcher dispatcher)
    {
        mainDispatcher = dispatcher;
    }

    public static bool IsOnMain
    {
        get
        {
            if (mainDispatcher is null) throw new NotSupportedException("you have to call Init() first");
            return mainDispatcher == Dispatcher.GetForCurrentThread();
        }
    }

    static void EnsureInvokeOnMainThread(Action action)
    {
        if (IsOnMain)
        {
            action();
        }
        else
        {
            mainDispatcher.Dispatch(action);
        }
    }

    public static Task Run(Action action)
    {
        var tcs = new TaskCompletionSource<object>();
        EnsureInvokeOnMainThread(() =>
        {
            try
            {
                action?.Invoke();
                tcs.SetResult(null);
            }
            catch (Exception ex)
            {
                tcs.SetException(ex);
            }
        });
        return tcs.Task;
    }
}

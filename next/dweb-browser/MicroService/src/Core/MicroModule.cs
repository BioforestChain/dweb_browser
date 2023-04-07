﻿
using DwebBrowser.MicroService.Sys.Dns;
using System.Net;
using System.Web;

namespace DwebBrowser.MicroService.Core;

public abstract class MicroModule : Ipc.MicroModuleInfo
{
    public abstract Mmid Mmid { get; init; }
    public Router? Router = null;

    private PromiseOut<bool> _runningStateLock = new PromiseOut<bool>();
    public bool Running { get => _runningStateLock.Value; }

    private async Task _beforeBootsStrap(IBootstrapContext bootstrapContext)
    {
        if (await _runningStateLock.WaitPromiseAsync())
        {
            throw new Exception($"module {Mmid} already running");
        }

        _runningStateLock = new PromiseOut<bool>();
        _bootstrapContext = bootstrapContext;
    }

    private IBootstrapContext? _bootstrapContext = null;
    public IBootstrapContext BootstrapContext { get => _bootstrapContext ?? throw new Exception("module no run."); }

    protected abstract Task _bootstrapAsync(IBootstrapContext bootstrapContext);

    private void _afterBootstrap(IBootstrapContext dnsMM) => _runningStateLock.Resolve(true);

    public async Task BootstrapAsync(IBootstrapContext bootstrapContext)
    {
        await _beforeBootsStrap(bootstrapContext);
        try
        {
            await _bootstrapAsync(bootstrapContext);
        }
        finally
        {
            _afterBootstrap(bootstrapContext);
        }
    }

    protected event Signal? _onAfterShutdown;

    protected async Task _beforeShutdownAsync()
    {
        if (!await _runningStateLock.WaitPromiseAsync())
        {
            throw new Exception($"module {Mmid} already shutdown");
        }

        _runningStateLock = new PromiseOut<bool>();

        /// 关闭所有的通讯
        _ipcSet.ToList().ForEach(async it => await it.Close());
        _ipcSet.Clear();
    }

    protected abstract Task _shutdownAsync();

    protected async Task _afterShutdownAsync()
    {
        await (_onAfterShutdown?.Emit()).ForAwait();
        _onAfterShutdown = null;
        _runningStateLock.Resolve(false);
        _bootstrapContext = null;
    }

    public async Task ShutdownAsync()
    {
        await _beforeShutdownAsync();

        try
        {
            await _shutdownAsync();
        }
        finally
        {
            await _afterShutdownAsync();
        }
    }

    /**
     * <summary>
     * 连接池
     * </summary>
     */
    protected HashSet<Ipc> _ipcSet = new();

    /**
     * <summary>
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     * </summary>
     */
    public event Signal<Ipc, HttpRequestMessage>? OnConnect;

    /**
     * <summary>
     * 尝试连接到指定对象
     * </summary>
     */
    public Task<ConnectResult> ConnectAsync(Mmid mmid, HttpRequestMessage? reason = null) =>
        _bootstrapContext!.Dns.Let(it =>
        {
            it.BootstrapAsync(mmid);
            return it.ConnectAsync(mmid);
        });

    /**
     * <summary>
     * 收到一个连接，触发相关事件
     * </summary>
     */
    public Task BeConnectAsync(Ipc ipc, HttpRequestMessage reason)
    {
        _ipcSet.Add(ipc);
        ipc.OnClose += async (_) => _ipcSet.Remove(ipc);
        ipc.OnEvent += async (ipcMessage, ipc, _) =>
        {
            if (ipcMessage.Name == "activity")
            {
                await _onActivityAsync(ipcMessage, ipc);
            }
        };

        return (OnConnect?.Emit(ipc, reason)).ForAwait();
    }

    protected abstract Task _onActivityAsync(IpcEvent Event, Ipc ipc);

    public async Task<HttpResponseMessage> NativeFetchAsync(HttpRequestMessage request)
    {
        foreach (var fetchAdapter in NativeFetch.NativeFetchAdaptersManager.Adapters)
        {
            var response = await fetchAdapter(this, request);

            if (response is not null)
            {
                return response;
            }
        }

        return _localeFileFetch(this, request) ?? await new HttpClient().SendAsync(request);
    }

    private HttpResponseMessage? _localeFileFetch(MicroModule remote, HttpRequestMessage request)
    {
        if (request.RequestUri is not null && request.RequestUri.Scheme == "file" && request.RequestUri.Host == "")
        {
            var query = HttpUtility.ParseQueryString(request.RequestUri.Query);

            var mode = query["mode"] ?? "auto";
            var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;
            var preRead = query["pre-read"]?.ToBooleanStrictOrNull() ?? false;

            var src = request.RequestUri.AbsolutePath.Substring(1);

            Console.WriteLine($"OPEN {src}");
            string dirname = null!;
            string filename = null!;

            src.LastIndexOf('/').Also(it =>
            {
                switch (it)
                {
                    case -1:
                        filename = src;
                        dirname = "";
                        break;
                    default:
                        filename = src.Substring(it + 1);
                        dirname = src.Substring(0, it + 1);
                        break;
                }
                src.Substring(0, it + 1);
            });

            // TODO: nativeFetch 本地文件读取未完成
            /// 尝试打开文件，如果打开失败就走 404 no found 响应

            return new HttpResponseMessage(HttpStatusCode.OK);
        }

        return null;
    }

    public Task<HttpResponseMessage> NativeFetchAsync(Uri url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, url));

    public Task<HttpResponseMessage> NativeFetchAsync(string url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, new Uri(url)));
}


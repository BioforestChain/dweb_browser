using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Sys.Http.Net
{
    public class HttpDwebServer
    {
        private MicroModule _nmm { get; init; }
        private DwebHttpServerOptions _options { get; init; }
        public HttpNMM.ServerStartResult StartResult { get; init; }

        public HttpDwebServer(MicroModule nmm, DwebHttpServerOptions options, HttpNMM.ServerStartResult startResult)
        {
            _nmm = nmm;
            _options = options;
            StartResult = startResult;
            var listenPo = new PromiseOut<ReadableStreamIpc>();

            Listen = Once.AsyncOnce(async () =>
            {
                /** 定义了路由的方法 */
                var routes = new Gateway.RouteConfig[]
                 {
                    new Gateway.RouteConfig("/", IpcMethod.Get),
                    new Gateway.RouteConfig("/", IpcMethod.Post),
                    new Gateway.RouteConfig("/", IpcMethod.Put),
                    new Gateway.RouteConfig("/", IpcMethod.Delete),
                    new Gateway.RouteConfig("/", IpcMethod.Patch),
                    new Gateway.RouteConfig("/", IpcMethod.Options),
                    new Gateway.RouteConfig("/", IpcMethod.Head),
                    new Gateway.RouteConfig("/", IpcMethod.Connect),
                    new Gateway.RouteConfig("/", IpcMethod.Trace)
                 };

                var ipc = await _nmm.ListenHttpDwebServer(StartResult, routes);
                listenPo.Resolve(ipc);
                return ipc;
            });
            Close = Once.AsyncOnce(async () =>
            {
                var ipc = await listenPo.WaitPromiseAsync();
                await ipc.Close();
                return await _nmm.CloseHttpDwebServer(_options);
            });
        }

        public Func<Task<ReadableStreamIpc>> Listen { get; init; }


        public Func<Task<bool>> Close { get; init; }
    }

    public record DwebHttpServerOptions(int? Port = 80, string? Subdomain = "");
}

namespace DwebBrowser.MicroService.Core
{
    public abstract partial class MicroModule
    {
        public async Task<HttpNMM.ServerStartResult> StartHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new URL("file://http.std.dweb/start")
                .SearchParamsSet("port", options.Port.ToString())
                .SearchParamsSet("subdomain", options.Subdomain)))
            .JsonAsync<HttpNMM.ServerStartResult>();

        public async Task<ReadableStreamIpc> ListenHttpDwebServer(
            HttpNMM.ServerStartResult startResult, Gateway.RouteConfig[] routes)
        {
            var streamIpc = new ReadableStreamIpc(this, string.Format("http-server/{0}", startResult.urlInfo.Host));
            var pureResponse = await NativeFetchAsync(
                new PureRequest(
                    new URL("file://http.std.dweb/listen")
                        .SearchParamsSet("token", startResult.token)
                        .SearchParamsSet("routes", JsonSerializer.Serialize(routes)).Href,
                    IpcMethod.Post,
                    Body: new PureStreamBody(streamIpc.ReadableStream.Stream)));

            streamIpc.BindIncomeStream(pureResponse.Body.ToStream());
            this.AddToIpcSet(streamIpc);
            return streamIpc;
        }

        public async Task<bool> CloseHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new URL("file://http.std.dweb/close")
                .SearchParamsSet("port", options.Port.ToString())
                .SearchParamsSet("subdomain", options.Subdomain)))
            .BoolAsync();

        public async Task<HttpDwebServer> CreateHttpDwebServer(DwebHttpServerOptions options) =>
            new HttpDwebServer(this, options, await StartHttpDwebServer(options));
    }
}




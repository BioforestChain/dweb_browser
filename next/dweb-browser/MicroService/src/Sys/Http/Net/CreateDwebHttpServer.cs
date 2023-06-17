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

            Close = Once.AsyncOnce(async () => await _nmm.CloseHttpDwebServer(_options));
        }

        public async Task<ReadableStreamIpc> Listen(Gateway.RouteConfig[]? routes = null)
        {
            if (routes is null)
            {
                routes = new Gateway.RouteConfig[]
                {
                    new Gateway.RouteConfig("", IpcMethod.Get),
                    new Gateway.RouteConfig("", IpcMethod.Post),
                    new Gateway.RouteConfig("", IpcMethod.Put),
                    new Gateway.RouteConfig("", IpcMethod.Delete),
                };
            }

            return await _nmm.ListenHttpDwebServer(StartResult, routes);
        }

        public Func<Task<bool>> Close { get; init; }
    }

    public record DwebHttpServerOptions(int? port = null, string? subdomain = null)
    {
        public int Port = port ?? 80;
        public string Subdomain = subdomain ?? "";
    };
}

namespace DwebBrowser.MicroService.Core
{
    public abstract partial class MicroModule
    {
        public async Task<HttpNMM.ServerStartResult> StartHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new URL("file://http.sys.dweb/start")
                .SearchParamsSet("port", options.port.ToString())
                .SearchParamsSet("subdomain", options.subdomain)))
            .JsonAsync<HttpNMM.ServerStartResult>();

        public async Task<ReadableStreamIpc> ListenHttpDwebServer(
            HttpNMM.ServerStartResult startResult, Gateway.RouteConfig[] routes)
        {
            var streamIpc = new ReadableStreamIpc(this, string.Format("http-server/{0}", startResult.urlInfo.Host));
            var pureResponse = await NativeFetchAsync(
                new PureRequest(
                    new URL("file://http.sys.dweb/listen")
                        .SearchParamsSet("host", startResult.urlInfo.Host)
                        .SearchParamsSet("token", startResult.token)
                        .SearchParamsSet("routes", JsonSerializer.Serialize(routes)).Href,
                    IpcMethod.Post,
                    Body: new PureStreamBody(streamIpc.ReadableStream.Stream)));

            streamIpc.BindIncomeStream(pureResponse.Body.ToStream());
            this.addToIpcSet(streamIpc);
            return streamIpc;
        }

        public async Task<bool> CloseHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new URL("file://http.sys.dweb/close")
                .SearchParamsSet("port", options.port.ToString())
                .SearchParamsSet("subdomain", options.subdomain)))
            .BoolAsync();

        public async Task<HttpDwebServer> CreateHttpDwebServer(DwebHttpServerOptions options) =>
            new HttpDwebServer(this, options, await StartHttpDwebServer(options));
    }
}




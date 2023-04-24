
using DwebBrowser.Helper;
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

    public record DwebHttpServerOptions(int port = 80, string subdomain = "");
}

namespace DwebBrowser.MicroService.Core
{
    public abstract partial class MicroModule
    {
        public async Task<HttpNMM.ServerStartResult> StartHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new Uri("file://http.sys.dweb/start")
                .AppendQuery("port", options.port.ToString())
                .AppendQuery("subdomain", options.subdomain)))
            .Json<HttpNMM.ServerStartResult>();

        public async Task<ReadableStreamIpc> ListenHttpDwebServer(
            HttpNMM.ServerStartResult startResult, Gateway.RouteConfig[] routes)
        {
            var streamIpc = new ReadableStreamIpc(this,$"http-server/{startResult.urlInfo.Host}");
            var res = await NativeFetchAsync(
                new HttpRequestMessage(
                    HttpMethod.Post,
                    new Uri("file://http.sys.dweb/listen")
                        .AppendQuery("host", startResult.urlInfo.Host)
                        .AppendQuery("token", startResult.token)
                        .AppendQuery("routes", JsonSerializer.Serialize(routes))
                    ).Also((it) => it.Content = new StreamContent(streamIpc.Stream.Stream))
                );

            streamIpc.BindIncomeStream(res.Stream());
            return streamIpc;
        }

        public async Task<bool> CloseHttpDwebServer(DwebHttpServerOptions options) =>
            await (await NativeFetchAsync(new Uri("file://http.sys.dweb/close")
                .AppendQuery("port", options.port.ToString())
                .AppendQuery("subdomain", options.subdomain)))
            .BoolAsync();

        public async Task<HttpDwebServer> CreateHttpDwebServer(DwebHttpServerOptions options) =>
            new HttpDwebServer(this, options, await StartHttpDwebServer(options));
    }
}




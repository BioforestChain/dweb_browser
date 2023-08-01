using System.Collections.Concurrent;
using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Http;
using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("DeskNMM");

    public override string Name { get; set; } = "Desk";
    public DeskNMM() : base("desk.browser.dweb")
    {
        s_controllerList.Add(new(this));
    }

    public ChangeableMap<Mmid, Ipc> RunningApps = new();

    private static readonly List<DeskController> s_controllerList = new();
    public static DeskController DeskController
    {
        get => s_controllerList.FirstOrDefault();
    }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Desktop,
    };

    public sealed class DesktopAppMetadata : MicroModuleManifest
    {
        [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
        public DesktopAppMetadata()
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
        {
            /// 给JSON反序列化用的空参数构造函数
        }

        public DesktopAppMetadata(
            Mmid mmid,
            IpcSupportProtocols ipcSupportProtocols,
            List<string> dweb_deeplinks,
            List<MicroModuleCategory> categories,
            string name,
            string? version = null,
            TextDirectionType? dir = null,
            string? lang = null,
            string? shortName = null,
            string? description = null,
            List<Core.ImageSource>? icons = null,
            List<Core.ImageSource>? screenshots = null,
            DisplayModeType? display = null,
            OrientationType? orientation = null,
            string? themeColor = null,
            string? backgroundColor = null,
            List<ShortcutItem>? shortcuts = null)
            : base(
                mmid,
                ipcSupportProtocols,
                dweb_deeplinks,
                categories,
                name,
                version,
                dir,
                lang,
                shortName,
                description,
                icons,
                screenshots,
                display,
                orientation,
                themeColor,
                backgroundColor,
                shortcuts)
        {
        }

        [JsonPropertyName("running")]
        public bool Running { get; set; } = false;

        /// <summary>
        /// 当前进程所拥有的窗口的状态
        /// </summary>
        [JsonPropertyName("winStates")]
        public List<WindowState> WinStates { get; set; } = new();

        public static DesktopAppMetadata FromMicroModuleManifest(IMicroModuleManifest mm)
        {
            return new DesktopAppMetadata(
                mm.Mmid, mm.IpcSupportProtocols, mm.Dweb_deeplinks, mm.Categories,
                mm.Name, mm.Version, mm.Dir, mm.Lang, mm.ShortName, mm.Description,
                mm.Icons, mm.Screenshots, mm.Display, mm.Orientation, mm.ThemeColor,
                mm.BackgroundColor, mm.Shortcuts);
        }

        public override string ToJson() => JsonSerializer.Serialize(this);
        public static DesktopAppMetadata? FromJson(string json) =>
            JsonSerializer.Deserialize<DesktopAppMetadata>(json);
    }

    public record ImageAccept(string accept);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        var taskbarServer = await CreateTaskbarWebServer();
        var desktopServer = await CreateDesktopWebServer();

        await MainThread.InvokeOnMainThreadAsync(async () => DeskController?.Create(taskbarServer, desktopServer));

        RunningApps.OnChange += async (map, _) =>
        {
            foreach (var app_id in map.Keys)
            {
                DeskController.TaskBarAppList.Add(new DeskStore.TaskApps(app_id, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()));
            }

            DeskStore.Instance.Save(DeskController.TaskBarAppList);
        };

        HttpRouter.AddRoute(IpcMethod.Get, "/readFile", async (request, _) =>
        {
            var url = request.QueryStringRequired("url");
            return await NativeFetchAsync(url);
        });

        HttpRouter.AddRoute(new Gateway.RouteConfig("/readAccept.", IpcMethod.Get), async (request, _) =>
        {
            var accept = request.Headers.Get("Accept") ?? "*/*";

            return new PureResponse(HttpStatusCode.OK, Body: new PureUtf8StringBody(JsonSerializer.Serialize(new ImageAccept(accept))));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/openAppOrActivate", async (request, _) =>
        {
            var app_id = request.QueryStringRequired("app_id");

            Console.Log("activity", app_id);
            var ipc = RunningApps.Get(app_id) ?? await ConnectAsync(app_id);

            if (ipc is not null)
            {
                await ipc.PostMessageAsync(IpcEvent.FromUtf8("activity", ""));
                /// 如果成功打开，将它“追加”到列表中
                await RunningApps.Remove(app_id);
                await RunningApps.Set(app_id, ipc);
                /// 如果应用关闭，将它从列表中移除
                ipc.OnClose += async (_) =>
                {
                    await RunningApps.Remove(app_id);
                };
            }

            return ipc is not null;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, _) =>
        {
            var app_id = request.QueryStringRequired("app_id");
            var closed = false;

            if (RunningApps.ContainsKey(app_id))
            {
                closed = await bootstrapContext.Dns.Close(app_id);
                if (closed)
                {
                    await RunningApps.Remove(app_id);
                }
            }

            return closed;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/desktop/apps", async (request, _) =>
        {
            return await DeskController.GetDesktopAppList();
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/desktop/observe/apps", async (request, ipc) =>
        {
            var stream = new ReadableStream(onStart: controller =>
            {
                Signal<ConcurrentDictionary<Mmid, Ipc>> cb = async (_, _) =>
                {
                    var apps = await DeskController.GetDesktopAppList();
                    Console.Log("/desktop/observe/apps", $"size={apps.Count}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(apps) + "\n").ToUtf8ByteArray());
                };

                RunningApps.OnChange += cb;

                ipc.OnClose += async (_) =>
                {
                    RunningApps.OnChange -= cb;
                    controller.Close();
                };
            });

            await RunningApps.OnChangeEmit();

            return new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(stream.Stream));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/apps", async (request, _) =>
        {
            var limit = request.SafeUrl.SearchParams.Get("limit")?.Let(it => it.ToIntOrNull()) ?? int.MaxValue;

            return await DeskController.GetTaskbarAppList(limit);
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/observe/apps", async (request, ipc) =>
        {
            var limit = request.SafeUrl.SearchParams.Get("limit")?.Let(it => it.ToIntOrNull()) ?? int.MaxValue;
            var stream = new ReadableStream(onStart: controller =>
            {
                Signal<ConcurrentDictionary<Mmid, Ipc>> cb = async (_, _) =>
                {
                    var apps = await DeskController.GetTaskbarAppList(limit);
                    Console.Log("/taskbar/observe/apps", $"size={apps.Count}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(apps) + "\n").ToUtf8ByteArray());
                };

                RunningApps.OnChange += cb;

                ipc.OnClose += async (_) =>
                {
                    RunningApps.OnChange -= cb;
                    controller.Close();
                };
            });

            await RunningApps.OnChangeEmit();

            return new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(stream.Stream));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/resize", async (request, _) =>
        {
            Console.Log("taskbar/resize", request.SafeUrl.SearchParams.ToString());
            var width = (int)request.SafeUrl.SearchParams.Get("width").Let(it => it.ToIntOrNull());
            var height = (int)request.SafeUrl.SearchParams.Get("height").Let(it => it.ToIntOrNull());

            return await MainThread.InvokeOnMainThreadAsync(() => DeskController.TaskBarResize(width, height));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/toggle-desktop-view", async (request, _) =>
        {
            return await MainThread.InvokeOnMainThreadAsync(() => DeskController?.ToggleDesktopView());
        });
    }

    public override void OpenActivity(string remoteMmid)
    {
        throw new NotImplementedException();
    }

    private async Task<HttpDwebServer> CreateTaskbarWebServer()
    {
        var server = await CreateHttpDwebServer(new DwebHttpServerOptions(433, "taskbar"));

        {
            var url = string.Empty;
            var serverIpc = await server.Listen();
            var API_PREFIX = "/api/";
            serverIpc.OnRequest += async (request, ipc, _) =>
            {
                var pathname = request.Uri.AbsolutePath;
                if (pathname.StartsWith(API_PREFIX))
                {
                    url = string.Format("file://{0}{1}", pathname[API_PREFIX.Length..], request.Uri.Query);
                    var mmid = new URL(url).Hostname;

                    if (mmid != Mmid)
                    {
                        /// 不支持
                        if ((await BootstrapContext.Dns.Query(mmid)) is null)
                        {
                            await ipc.PostMessageAsync(
                            IpcResponse.FromText(
                                request.ReqId,
                                404,
                                /// 加入跨域支持
                                new IpcHeaders(request.Headers),
                                $"{{error:'no support {mmid}'}}",
                                ipc));
                        }
                    }
                }
                else
                {
                    url = string.Format($"file:///sys/browser/desk{pathname}?mode=stream");
                }

                var response = await NativeFetchAsync(new PureRequest(url, request.Method, request.Headers, request.Body.ToPureBody()));
                var ipcReponse = response.ToIpcResponse(request.ReqId, ipc);

                await ipc.PostMessageAsync(ipcReponse);
            };
        }

        return server;
    }

    private async Task<HttpDwebServer> CreateDesktopWebServer()
    {
        var server = await CreateHttpDwebServer(new DwebHttpServerOptions(433, "desktop"));

        {
            var url = string.Empty;
            var serverIpc = await server.Listen();
            var API_PREFIX = "/api/";
            serverIpc.OnRequest += async (request, ipc, _) =>
            {
                var pathname = request.Uri.AbsolutePath;
                if (pathname.StartsWith(API_PREFIX))
                {
                    url = string.Format("file://{0}{1}", pathname[API_PREFIX.Length..], request.Uri.Query);
                }
                else
                {
                    url = string.Format($"file:///sys/browser/desk{pathname}?mode=stream");
                }

                var response = await NativeFetchAsync(new PureRequest(url, request.Method, request.Headers, request.Body.ToPureBody()));
                var ipcReponse = response.ToIpcResponse(request.ReqId, ipc);

                await ipc.PostMessageAsync(ipcReponse);
            };
        }

        return server;
    }
}

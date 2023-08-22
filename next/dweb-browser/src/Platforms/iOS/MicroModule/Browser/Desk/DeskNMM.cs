using System.Collections.Concurrent;
using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Http;
using DwebBrowser.MicroService.Sys.Http;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskNMM : IOSNativeMicroModule
{
    static readonly Debugger Console = new("DeskNMM");

    public DeskNMM() : base("desk.browser.dweb", "Desk")
    {
        //s_controllerList.Add(new(this));
        DeskController = new DeskController(this);
    }

    public ChangeableMap<Mmid, Ipc> RunningApps = new();

    public static readonly ConcurrentDictionary<UUID, DeskUIView> DeskAppControllerMap = new();
    public static DeskController DeskController = null!;

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

        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            if (DeskController is not null)
            {
                DeskController.TaskbarServer = taskbarServer;
                DeskController.DesktopServer = desktopServer;
                await DeskController.Create();
            }
        });

        RunningApps.OnChangeAdd(async (map, _) =>
        {
            foreach (var app_id in map.Keys)
            {
                var taskApp = new TaskAppsStore.TaskApps(app_id, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());
                if (!DeskController.TaskBarAppList.Contains(taskApp))
                {
                    DeskController.TaskBarAppList.Add(taskApp);
                }
            }

            TaskAppsStore.Instance.Save(DeskController.TaskBarAppList);
        });

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

            Console.Log("/openAppOrActivate", app_id);

            try
            {
                var ipc = RunningApps.Get(app_id) ?? await ConnectAsync(app_id);
                await ipc.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.Activity.Event, ""));

                /// 如果成功打开，将它“追加”到列表中
                if (!RunningApps.ContainsKey(app_id))
                {
                    RunningApps.TryAdd(app_id, ipc);
                }

                /// 如果应用关闭，将它从列表中移除
                ipc.OnClose += async (_) =>
                {
                    RunningApps.Remove(app_id);
                };

                /// 将所有的窗口聚焦，这个行为不依赖于 Activity 事件，而是Desk模块自身托管窗口的行为
                await DeskController.DesktopWindowsManager.FocusWindow(app_id);

                return true;
            }
            catch (Exception e)
            {
                Console.Error("/openAppOrActivate", e.Message);
                return false;
            }
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/toggleMaximize", async (request, _) =>
        {
            var mmid = request.QueryStringRequired("app_id");
            return DeskController.DesktopWindowsManager.ToggleMaximize(mmid);
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
                    RunningApps.Remove(app_id);
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
                Signal<ChangeableMap<Mmid, Ipc>> cb = async (_, _) =>
                {
                    var apps = await DeskController.GetDesktopAppList();
                    Console.Log("/desktop/observe/apps", $"size={apps.Count}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(apps) + "\n").ToUtf8ByteArray());
                };

                RunningApps.OnChangeAdd(cb);

                ipc.OnClose += async (_) =>
                {
                    RunningApps.OnChangeRemove(cb);
                    controller.Close();
                };
            });

            RunningApps.OnChangeEmit();

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
                Signal<ChangeableMap<Mmid, Ipc>> cb = async (_, _) =>
                {
                    var apps = await DeskController.GetTaskbarAppList(limit);
                    Console.Log("/taskbar/observe/apps", $"size={apps.Count}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(apps) + "\n").ToUtf8ByteArray());
                };

                RunningApps.OnChangeAdd(cb);

                ipc.OnClose += async (_) =>
                {
                    RunningApps.OnChangeRemove(cb);
                    controller.Close();
                };
            });

            RunningApps.OnChangeEmit();

            return new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(stream.Stream));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/observe/status", async (request, ipc) =>
        {
            var stream = new ReadableStream(onStart: controller =>
            {
                Signal<DeskController.TaskBarState> cb = async (status, _) =>
                {
                    Console.Log("/taskbar/observe/status", $"focus: {status.focus}, appId: {status.appId}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(status) + "\n").ToUtf8ByteArray());
                };
                DeskController.OnTaskbarListener.OnListener += cb;

                ipc.OnClose += async (_) =>
                {
                    DeskController.OnTaskbarListener.OnListener -= cb;
                    controller.Close();
                };
            });

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

        OnActivity += async (Event, ipc, _) =>
        {
            await OpenActivity();
        };

        DeskController.OnActivity.OnListener += async (_) =>
        {
            await OpenActivity();
        };
    }

    internal Task OpenActivity() => MainThread.InvokeOnMainThreadAsync(async () =>
    {
        DeskAppUIView.Start();
    });

    private const string API_PREFIX = "/api/";

    private async Task<HttpDwebServer> CreateTaskbarWebServer()
    {
        var server = await CreateHttpDwebServer(new DwebHttpServerOptions(433, "taskbar"));

        {
            var url = string.Empty;
            var serverIpc = await server.Listen();
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

    private async Task<HttpDwebServer> CreateDesktopWebServer()
    {
        var server = await CreateHttpDwebServer(new DwebHttpServerOptions(433, "desktop"));

        {
            var url = string.Empty;
            var serverIpc = await server.Listen();
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

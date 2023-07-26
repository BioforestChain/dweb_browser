using System.Text.Json;
using System.Text.Json.Serialization;
using System.Collections.Concurrent;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DesktopNMM");

    public new const string Name = "Desk";
    public DeskNMM() : base("desk.browser.dweb")
    {
    }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Application,
        MicroModuleCategory.Desktop,
    };

    public class DesktopAppMetadata : MicroModuleManifest
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

        public override string ToJson() => JsonSerializer.Serialize(this);
        public static DesktopAppMetadata? FromJson(string json) =>
            JsonSerializer.Deserialize<DesktopAppMetadata>(json);
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {

        var taskbarAppList = DeskStore.Instance.All();
        var runningApps = new ChangeableMap<Mmid, Ipc>();

        runningApps.OnChange += async (map, _) =>
        {
            foreach (var app_id in map.Keys)
            {
                taskbarAppList.Add(new DeskStore.TaskApps(app_id, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()));
            }

            DeskStore.Instance.Save(taskbarAppList);
        };

        async Task<List<DesktopAppMetadata>> getDesktopAppList()
        {
            var apps = await bootstrapContext.Dns.Search(MicroModuleCategory.Application);
            List<DesktopAppMetadata> desktopApps = new();

            foreach (var app in apps)
            {
                desktopApps.Add(new DesktopAppMetadata(
                    app.Mmid, app.IpcSupportProtocols, app.Dweb_deeplinks, app.Categories,
                    app.Name, app.Version, app.Dir, app.Lang, app.ShortName, app.Description,
                    app.Icons, app.Screenshots, app.Display, app.Orientation, app.ThemeColor,
                    app.BackgroundColor, app.Shortcuts)
                {
                    Running = runningApps.ContainsKey(app.Mmid)
                });
            }

            return desktopApps;
        }

        async Task<List<DesktopAppMetadata>> getTaskbarAppList(int limit)
        {
            List<DesktopAppMetadata> apps = new();

            foreach (var app in taskbarAppList)
            {
                var metadata = await bootstrapContext.Dns.Query(app.Mmid);
                if (metadata is not null)
                {
                    apps.Add(new DesktopAppMetadata(
                    metadata.Mmid, metadata.IpcSupportProtocols, metadata.Dweb_deeplinks, metadata.Categories,
                    metadata.Name, metadata.Version, metadata.Dir, metadata.Lang, metadata.ShortName, metadata.Description,
                    metadata.Icons, metadata.Screenshots, metadata.Display, metadata.Orientation, metadata.ThemeColor,
                    metadata.BackgroundColor, metadata.Shortcuts)
                    {
                        Running = runningApps.ContainsKey(metadata.Mmid)
                    });
                }

                if (apps.Count >= limit)
                {
                    return apps;
                }
            }

            return apps;
        }

        HttpRouter.AddRoute(IpcMethod.Get, "/openAppOrActivate", async (request, _) =>
        {
            var app_id = request.QueryStringRequired("app_id");

            Console.Log("activity", app_id);
            var ipc = runningApps.Get(app_id) ?? await ConnectAsync(app_id);

            if (ipc is not null)
            {
                await ipc.PostMessageAsync(IpcEvent.FromUtf8("activity", ""));
                /// 如果成功打开，将它“追加”到列表中
                await runningApps.Remove(app_id);
                await runningApps.Set(app_id, ipc);
                /// 如果应用关闭，将它从列表中移除
                ipc.OnClose += async (_) =>
                {
                    await runningApps.Remove(app_id);
                };
            }

            return ipc is not null;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/closeApp", async (request, _) =>
        {
            var app_id = request.QueryStringRequired("app_id");
            var closed = false;

            if (runningApps.ContainsKey(app_id))
            {
                closed = await bootstrapContext.Dns.Close(app_id);
                if (closed)
                {
                    await runningApps.Remove(app_id);
                }
            }

            return closed;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/desktop/apps", async (request, _) =>
        {
            return await getDesktopAppList();
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/desktop/observe/apps", async (request, ipc) =>
        {
            return new ReadableStream(onStart: controller =>
            {
                Signal<ConcurrentDictionary<Mmid, Ipc>> cb = async (_, _) =>
                {
                    var apps = await getDesktopAppList();
                    Console.Log("/desktop/observe/apps", $"size={apps.Count}");
                    await controller.EnqueueAsync((JsonSerializer.Serialize(apps) + "\n").ToUtf8ByteArray());
                };

                runningApps.OnChange += cb;

                ipc.OnClose += async (_) =>
                {
                    runningApps.OnChange -= cb;
                    controller.Close();
                };
            });
        });

        //HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/apps", async (request, _) =>
        //{
        //    var limit = request.SafeUrl.SearchParams.Get("limit")?.Let(it => it.ToIntOrNull()) ?? int.MaxValue;

        //    return await getTaskbarAppList(limit);
        //});

        //HttpRouter.AddRoute(IpcMethod.Get, "/taskbar/observe/apps", async (request, ipc) =>
        //{
        //    return new ReadableStream(onStart: controller =>
        //    {

        //    });
        //});
    }
}



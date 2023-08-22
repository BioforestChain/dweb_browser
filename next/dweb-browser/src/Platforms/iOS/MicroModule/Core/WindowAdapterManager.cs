using System.Collections.Concurrent;
using DwebBrowser.MicroService.Browser.Desk;

namespace DwebBrowser.MicroService.Core;

using CreateWindowAdapter = Func<WindowState, Task<WindowController>>;

public record WindowRenderScope(float Width, float Height, float Scale);

public class WindowAdapterManager : AdapterManager<CreateWindowAdapter>
{
    public static readonly WindowAdapterManager Instance = new();

    public ConcurrentDictionary<UUID, Action<WindowRenderScope, DeskAppUIView>> RenderProviders = new();

    public async Task<WindowController> CreateWindow(WindowState winState)
    {
        foreach (var adapter in Adapters)
        {
            var winCtrl = await adapter(winState);
            if (winCtrl is not null)
            {
                /// 窗口创建成功，将窗口保存到实例集合中
                WindowInstancesManager.WindowInstancesManagerInstance.Instances.Set(winCtrl.Id, winCtrl);
                return winCtrl;
            }
        }

        throw new Exception($"no support create native window, owner: {winState.Owner} provider: {winState.Provider}");
    }
}

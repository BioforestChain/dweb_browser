namespace DwebBrowser.MicroService.Core;

public class WindowInstancesManager
{
    public static WindowInstancesManager WindowInstancesManagerInstance = new();

    /// <summary>
    /// 所有的窗口实例
    /// </summary>
    public ChangeableMap<UUID, WindowController> Instances = new();

    public WindowController? Get(UUID id) => Instances.GetValueOrDefault(id);

    public void Add(WindowController window)
    {
        Instances.TryAdd(window.Id, window);
        window.OnClose.OnListener += async (_, _) =>
        {
            Instances.Remove(window.Id);
        };
    }
}


namespace DwebBrowser.MicroService.Core;

public class WindowInstancesManager
{
    public static WindowInstancesManager WindowInstancesManagerInstance = new();

    public ChangeableMap<UUID, WindowController> Instances = new();
}


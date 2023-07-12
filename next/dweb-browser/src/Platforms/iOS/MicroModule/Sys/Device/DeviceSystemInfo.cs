using Foundation;

namespace DwebBrowser.MicroService.Sys.Device;

public static class DeviceSystemInfo
{
    static readonly Debugger Console = new("DeviceSystemInfo");
    private static readonly NSUserDefaults _userDefaults = NSUserDefaults.StandardUserDefaults;
    public static readonly string UUID_KEY = "UUID";

    public record DeviceInfoRecord(
        string platform,
        string version,
        string model,
        string manufacturer,
        string name,
        string uuid
    );

    public static string GetUUID()
	{
        var UUID = _userDefaults.StringForKey(UUID_KEY);
        Console.Log("GetUUID", "{0}", string.IsNullOrWhiteSpace(UUID));

        if (string.IsNullOrWhiteSpace(UUID))
        {
            UUID = Guid.NewGuid().ToString();
            _userDefaults.SetString(UUID, UUID_KEY);
        }

        return UUID;
    }

    public static DeviceInfoRecord GetDeviceInfo()
    {
        Console.Log("DeviceInfo.Platform", "" + DeviceInfo.Current.Platform);
        Console.Log("DeviceInfo.VersionString", "" + DeviceInfo.Current.VersionString);
        Console.Log("DeviceInfo.Model", DeviceInfo.Current.Model);
        Console.Log("DeviceInfo.Manufacturer", DeviceInfo.Current.Manufacturer);
        Console.Log("DeviceInfo.Name", DeviceInfo.Current.Name);

        return new DeviceInfoRecord(
            DeviceInfo.Current.Platform.ToString(),
            DeviceInfo.Current.VersionString.ToString(),
            DeviceInfo.Current.Model.ToString(),
            DeviceInfo.Current.Manufacturer.ToString(),
            DeviceInfo.Current.Name.ToString(),
            GetUUID()
        );
    }
}


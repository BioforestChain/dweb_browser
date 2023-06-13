using System.IO;
using Foundation;
using DwebBrowser.MicroService.Browser.Jmm;

namespace DwebBrowser.Helper;

public static class PathHelper
{
    private const string ASSETS = "Assets";

    /// <summary>
    /// 获取 iOS App 根目录
    /// </summary>
    /// <returns></returns>
    public static string GetIOSAppRootDirectory() => AppContext.BaseDirectory;

    /// <summary>
    /// iOS App 资源文件目录
    /// </summary>
    /// <returns></returns>
    public static string GetIOSAppAssetsPath() => Path.Combine(GetIOSAppRootDirectory(), ASSETS);

    /// <summary>
    /// 获取 iOS 资源目录
    /// </summary>
    public static string GetIOSDocumentDirectory() =>
        NSSearchPath.GetDirectories(NSSearchPathDirectory.DocumentDirectory, NSSearchPathDomain.User, true).First();
}


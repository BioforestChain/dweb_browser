
using System.IO;
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
}


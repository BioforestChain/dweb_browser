using System.Text.Json;
using DwebBrowser.MicroService.Browser.NativeUI.Base;
using DwebBrowser.MicroService.Browser.NativeUI.SafeArea;
using DwebBrowser.MicroService.Browser.NativeUI.StatusBar;
using DwebBrowser.MicroService.Browser.NativeUI.NavigationBar;
using DwebBrowser.MicroService.Browser.NativeUI.VirtualKeyboard;
using DwebBrowser.MicroService.Http;

#nullable enable

namespace DwebBrowser.Helper;

public class QueryHelper
{
    static QueryHelper()
    {
        ResponseRegistry.RegistryJsonAble<StatusBarController>(
            typeof(StatusBarController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<NavigationBarController>(
            typeof(NavigationBarController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<VirtualKeyboardController>(
            typeof(VirtualKeyboardController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<SafeAreaController>(
            typeof(SafeAreaController), it => it.GetState());
    }

}
public static class PureRequestQueryExtensions
{

    public static ColorJson? QueryColor(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        return JsonSerializer.Deserialize<ColorJson>(it)!;
    });

    public static BarStyle? QueryStyle(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        return new BarStyle(it.ToUpper());
    });

    public static bool? QueryBool(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.ToBooleanStrictOrNull();
    public static string? QueryString(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key);
    public static string QueryStringRequired(this PureRequest req, string key) => QueryString(req, key) ?? throw new KeyNotFoundException("require query." + key);
}

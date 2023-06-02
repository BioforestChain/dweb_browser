using Microsoft.Extensions.Logging;
using System.Text.Json;



namespace DwebBrowser;

public static class MauiProgram
{
    static readonly Debugger Console = new("MauiProgram");
    public static MauiApp CreateMauiApp()
    {
        Console.Log("CreateMauiApp", "Start");
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        return builder.Build();
    }
}


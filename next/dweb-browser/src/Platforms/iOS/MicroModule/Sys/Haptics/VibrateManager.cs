using AudioToolbox;
using BrowserFramework;
using CoreHaptics;
using Foundation;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Haptics;

public static class VibrateManager
{
    static readonly Debugger Console = new("VibrateManager");
    static bool s_isSupportHaptics = CHHapticEngine.GetHardwareCapabilities().SupportsHaptics;

    public enum VibrateType
    {
        Click,
        Disabled,
        DoubleClick,
        HeavyClick,
        Tick,
        Customize
    }

    public static async Task<bool> VibrateAsync(VibrateType type, NSNumber[]? durationArr = null)
    {
        if (!s_isSupportHaptics)
        {
            Console.Log("SupportsHaptics", "No support haptics");
            return false;
        }
        

        switch (type)
        {
            case VibrateType.Click:
                durationArr = new NSNumber[] { 1 };
                break;
            case VibrateType.Disabled:
                durationArr = new NSNumber[] { 1, 63, 1, 119, 1, 129, 1 };
                break;
            case VibrateType.DoubleClick:
                durationArr = new NSNumber[] { 10, 1 };
                break;
            case VibrateType.HeavyClick:
                durationArr = new NSNumber[] { 1, 100, 1, 1 };
                break;
            case VibrateType.Tick:
                durationArr = new NSNumber[] { 10, 999, 1, 1 };
                break;
            case VibrateType.Customize:
                if (durationArr is null)
                {
                    Console.Log("Vibrate", "custom vibrate duration is null");
                    return false;
                }
                break;
        }

        try
        {
            HapticsHelper.VibrateWithDurationArr(durationArr!);
        }
        catch (Exception e)
        {
            Console.Error("Vibrate", "exception: {0}", e.Message);
            await SystemSound.Vibrate.PlayAlertSoundAsync();
            return false;
        }

        return true;
    }
}


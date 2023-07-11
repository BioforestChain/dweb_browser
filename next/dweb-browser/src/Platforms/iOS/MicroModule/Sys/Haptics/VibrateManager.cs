using AudioToolbox;
using DwebBrowserFramework;
using CoreHaptics;
using Foundation;
using UIKit;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Haptics;

public static class VibrateManager
{
    static readonly Debugger Console = new("VibrateManager");
    static readonly bool s_isSupportHaptics = CHHapticEngine.GetHardwareCapabilities().SupportsHaptics;

    public enum VibrateType
    {
        Click,
        Disabled,
        DoubleClick,
        HeavyClick,
        Tick,
        Customize
    }

    public static async Task ImpactAsync(string? style)
    {
        var impactStyle = style switch
        {
            string s when s.EqualsIgnoreCase("MEDIUM") => UIImpactFeedbackStyle.Medium,
            string s when s.EqualsIgnoreCase("HEAVY") => UIImpactFeedbackStyle.Heavy,
            _ => UIImpactFeedbackStyle.Light
        };

        await MainThread.InvokeOnMainThreadAsync(() =>
        {
            // 初始化反馈
            var impact = new UIImpactFeedbackGenerator(impactStyle);
            // 通知系统即将发生触觉反馈
            impact.Prepare();

            // 触发反馈
            impact.ImpactOccurred();
        });
    }

    public static async Task NotificationAsync(string? style)
    {
        var type = style switch
        {
            string t when t.EqualsIgnoreCase("SUCCESS") => UINotificationFeedbackType.Success,
            string t when t.EqualsIgnoreCase("WARNING") => UINotificationFeedbackType.Warning,
            _ => UINotificationFeedbackType.Error,
        };

        await MainThread.InvokeOnMainThreadAsync(() =>
        {
            // 初始化反馈
            var notification = new UINotificationFeedbackGenerator();
            // 通知系统即将发生触觉反馈
            notification.Prepare();
            // 触发反馈
            notification.NotificationOccurred(type);
        });
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


using CoreHaptics;
using AudioToolbox;

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

    public static async Task<bool> VibrateAsync(VibrateType type, double[]? durationArr = null)
    {
        if (!s_isSupportHaptics)
        {
            Console.Log("SupportsHaptics", "No support haptics");
            return false;
        }

        switch (type)
        {
            case VibrateType.Click:
                durationArr = new double[] { 1 };
                break;
            case VibrateType.Disabled:
                durationArr = new double[] { 1, 63, 1, 119, 1, 129, 1 };
                break;
            case VibrateType.DoubleClick:
                durationArr = new double[] { 10, 1 };
                break;
            case VibrateType.HeavyClick:
                durationArr = new double[] { 1, 100, 1, 1 };
                break;
            case VibrateType.Tick:
                durationArr = new double[] { 10, 999, 1, 1 };
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
            // 创建引擎
            var engine = new CHHapticEngine(out var error);

            if (error is not null)
            {
                Console.Log("Vibrate", "error: {0}", error.LocalizedDescription);
                await SystemSound.Vibrate.PlayAlertSoundAsync();
                return false;
            }

            await engine.StartAsync();

            engine.ResetHandler = async () =>
            {
                try
                {
                    await engine.StartAsync();
                }
                catch
                {
                    await SystemSound.Vibrate.PlayAlertSoundAsync();
                }
            };


            var eventsList = new List<CHHapticEvent>();
            var i = 0;
            var relativeTime = 0.0;
            foreach (var duration in durationArr!)
            {
                if (i % 2 == 0)
                {
                    var intensity = new CHHapticEventParameter(CHHapticEventParameterId.HapticIntensity, (float)0.5);
                    var sharpness = new CHHapticEventParameter(CHHapticEventParameterId.HapticSharpness, (float)0.6);
                    var continuousEvent = new CHHapticEvent(
                        CHHapticEventType.HapticContinuous,
                        new[] { intensity, sharpness },
                        relativeTime,
                        Math.Max(0.01, duration / 1000));
                    eventsList.Append(continuousEvent);
                }

                Console.Log("Vibrate", "relativeTime: {0}, duration: {1}", relativeTime, duration / 1000);
                relativeTime += duration / 1000;
            }

            var pattern = new CHHapticPattern(eventsList.ToArray(), new CHHapticDynamicParameter[0], out var outError);

            if (outError is not null)
            {
                Console.Log("Vibrate", "outError: {0}", outError.LocalizedDescription);
                await SystemSound.Vibrate.PlayAlertSoundAsync();
                return false;
            }

            var player = engine.CreateAdvancedPlayer(pattern, out var createError);

            if (createError is not null)
            {
                Console.Log("Vibrate", "createError: {0}", createError.LocalizedDescription);
                await SystemSound.Vibrate.PlayAlertSoundAsync();
                return false;
            }

            player.Start(0, out var startError);

            if (startError is not null)
            {
                Console.Log("Vibrate", "startError: {0}", startError.LocalizedDescription);
                await SystemSound.Vibrate.PlayAlertSoundAsync();
                return false;
            }
        }
        catch (Exception e)
        {
            Console.Log("Vibrate", "exception: {0}", e.Message);
            await SystemSound.Vibrate.PlayAlertSoundAsync();
            return false;
        }

        return true;
    }
}


using UIKit;
using System.Net;
using System.Text.Json;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Sys.Haptics;

public class HapticsNMM : NativeMicroModule
{
    public HapticsNMM() : base("haptics.sys.dweb")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// 触碰轻质量物体
        HttpRouter.AddRoute(IpcMethod.Get, "/impactLight", async (request, _) =>
        {
            var style = request.QueryString("style") switch
            {
                string s when s.EqualsIgnoreCase("MEDIUM") => UIImpactFeedbackStyle.Medium,
                string s when s.EqualsIgnoreCase("HEAVY") => UIImpactFeedbackStyle.Heavy,
                _ => UIImpactFeedbackStyle.Light
            };

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                // 初始化反馈
                var impact = new UIImpactFeedbackGenerator(style);
                // 通知系统即将发生触觉反馈
                impact.Prepare();

                // 触发反馈
                impact.ImpactOccurred();
            });

            return new PureResponse(HttpStatusCode.OK);
        });

        /// 警告分隔的振动通知
        HttpRouter.AddRoute(IpcMethod.Get, "/notification", async (request, _) =>
        {
            var type = request.QueryStringRequired("duration") switch
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
            return new PureResponse(HttpStatusCode.OK);
        });

        /// 单击手势的反馈振动
        HttpRouter.AddRoute(IpcMethod.Get, "/vibrateClick", async (request, _) =>
        {
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.Click))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });

        /// 禁用手势的反馈振动，与headShak特效一致
        HttpRouter.AddRoute(IpcMethod.Get, "/vibrateDisabled", async (request, _) =>
        {
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.Disabled))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });

        /// 双击手势的反馈振动
        HttpRouter.AddRoute(IpcMethod.Get, "/vibrateDoubleClick", async (request, _) =>
        {
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.DoubleClick))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });

        /// 重击手势的反馈振动，比如菜单键/长按/3DTouch
        HttpRouter.AddRoute(IpcMethod.Get, "/vibrateHeavyClick", async (request, _) =>
        {
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.HeavyClick))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });

        /// 滴答
        HttpRouter.AddRoute(IpcMethod.Get, "/vibrateTick", async (request, _) =>
        {
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.Tick))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });

        /// 自定义传递 振动频率
        HttpRouter.AddRoute(IpcMethod.Get, "/customize", async (request, _) =>
        {
            var duration = request.QueryStringRequired("duration");
            var durationType = typeof(List<double>)!;
            var durationList = (List<double>)JsonSerializer.Deserialize(duration, durationType)!;

            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.Customize, durationList.ToArray()))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });
    }
}


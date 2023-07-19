using System.Net;
using System.Text.Json;
using DwebBrowser.MicroService.Http;
using Foundation;

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
            await VibrateManager.ImpactAsync(request.QueryString("style"));

            return new PureResponse(HttpStatusCode.OK);
        });

        /// 警告分隔的振动通知
        HttpRouter.AddRoute(IpcMethod.Get, "/notification", async (request, _) =>
        {
            await VibrateManager.NotificationAsync(request.QueryString("style"));

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
            var durationArr = JsonSerializer.Deserialize<double[]>(duration);
            var durationList = Array.ConvertAll(durationArr, x => new NSNumber(x));
            if (await VibrateManager.VibrateAsync(VibrateManager.VibrateType.Customize, durationList))
            {
                return new PureResponse(HttpStatusCode.OK);
            }

            return new PureResponse(HttpStatusCode.InternalServerError);
        });
    }
}


using UIKit;
using CoreGraphics;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Sys.NativeUI;
using DwebBrowser.MicroService.Sys.Mwebview;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Helper;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.SafeArea;

public class SafeAreaController : AreaController, IToJsonAble
{
    static Debugger Console = new("SafeAreaController");

    // Notch 刘海 or Dynamic Island 灵动岛
    public State<AreaJson> CutoutInsets { get; init; }

    // 外部区域
    public State<AreaJson> OuterAreaInsets { get; init; }

    public readonly State<SafeAreaState> Observer;
    public StateObservable<SafeAreaState> StateObserver { get; init; }

    public NativeUiController NativeUiController { get; init; }

    public SafeAreaController(
        MultiWebViewController mwebviewController,
        NativeUiController nativeUiController) : base(
        overlayState: new(true),
        areaState: new(CGRect.Empty.ToAreaJson()))
    {
        CutoutInsets = new(new AreaJson(UIApplication.SharedApplication.KeyWindow.SafeAreaInsets.Top, 0, 0, 0));
        var webviewFrame = mwebviewController.WebviewFrame.Get();
        OuterAreaInsets = new(new AreaJson(
            webviewFrame.Top.Value,
            0,
            0,
            mwebviewController.webviewContainer.Frame.GetMaxY().Value
            - webviewFrame.Bottom.Value));
        Observer = new State<SafeAreaState>(() => GetState());
        StateObserver = new(() => ToJson());
        NativeUiController = nativeUiController;

        var statusBar = NativeUiController.StatusBar;
        var navigationBar = NativeUiController.NavigationBar;
        var virtualKeyboard = NativeUiController.VirtualKeyboard;
        var safeArea = this;

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                var isSafeAreaOverlay = value.Overlay;
                var safeAreaCutoutInsets = safeArea.CutoutInsets.Get();

                var statusBarState = statusBar.GetState();
                var isStatusBarOverlay = statusBarState.Overlay;
                var statusBarInsets = statusBarState.Area;

                var navigationBarState = navigationBar.GetState();
                var isNavigationBarOverlay = navigationBarState.Overlay;
                var navigationBarInsets = navigationBarState.Area;

                var virtualKeyboardState = virtualKeyboard.GetState();
                var isVirtualKeyboardOverlay = virtualKeyboardState.Overlay;
                var virtualKeyboardInsets = virtualKeyboardState.Area;

                var RES_safeAreaInsets = AreaJson.Empty;
                var RES_outerAreaInsets = AreaJson.Empty;

                /// 顶部有状态栏、刘海或灵动岛区域
                var topInsets = statusBarInsets.Union(safeAreaCutoutInsets);

                if (isStatusBarOverlay && isSafeAreaOverlay)
                {
                    // 都覆盖，那么就写入safeArea，outerArea不需要调整
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(topInsets);
                }
                else if (isStatusBarOverlay && !isSafeAreaOverlay)
                {
                    // outerArea写入刘海区域，safeArea只写剩余的
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(safeAreaCutoutInsets);
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(topInsets.Exclude(safeAreaCutoutInsets));
                }
                else if (!isStatusBarOverlay && isSafeAreaOverlay)
                {
                    // outerArea写入状态栏，safeArea只写剩余的
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(statusBarInsets);
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(topInsets.Exclude(statusBarInsets));
                }
                else
                {
                    // 都不覆盖，全部写入 outerArea
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(topInsets);
                }

                /// 底部，底部有导航栏和虚拟键盘
                var bottomInsets = navigationBarInsets.Union(virtualKeyboardInsets);

                if (isVirtualKeyboardOverlay && isNavigationBarOverlay)
                {
                    // 都覆盖，那么就写入safeArea，outerArea不需要调整
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(bottomInsets);
                }
                else if (isVirtualKeyboardOverlay && !isNavigationBarOverlay)
                {
                    // outerArea写入导航栏，safeArea只写剩余的
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(navigationBarInsets);
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(bottomInsets.Exclude(navigationBarInsets));
                }
                else if (!isVirtualKeyboardOverlay && isNavigationBarOverlay)
                {
                    // outerArea写入虚拟键盘，safeArea只写剩余的
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(virtualKeyboardInsets);
                    RES_safeAreaInsets = RES_safeAreaInsets.Add(bottomInsets.Exclude(virtualKeyboardInsets));
                }
                else
                {
                    // 都不覆盖，全部写入 outerArea
                    RES_outerAreaInsets = RES_outerAreaInsets.Add(bottomInsets);
                }

                AreaState.Set(RES_safeAreaInsets);
                OuterAreaInsets.Set(RES_outerAreaInsets);
                mwebviewController.WebviewFrame.Set(new CGRect(
                    new nfloat(RES_outerAreaInsets.left),
                    new nfloat(RES_outerAreaInsets.top),
                    mwebviewController.webviewContainer.Frame.Width,
                    new nfloat(
                        mwebviewController.webviewContainer.Frame.GetMaxY().Value
                        - RES_outerAreaInsets.bottom
                        - RES_outerAreaInsets.top)));
            });

            await StateObserver.EmitAsync();
        };
    }

    public SafeAreaState GetState()
    {
        return new SafeAreaState
        {
            CutoutInsets = CutoutInsets.Get(),
            OuterAreaInsets = OuterAreaInsets.Get(),
            Overlay = OverlayState.Get(),
            Area = AreaState.Get()
        };
    }


    public string ToJson()
    {
        return JsonSerializer.Serialize(GetState());
    }
}

public class SafeAreaState : AreaState
{
    [JsonPropertyName("cutoutInsets")]
    public AreaJson CutoutInsets { get; set; }
    [JsonPropertyName("outerInsets")]
    public AreaJson OuterAreaInsets { get; set; }
}
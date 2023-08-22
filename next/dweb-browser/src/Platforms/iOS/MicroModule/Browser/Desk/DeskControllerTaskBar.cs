using CoreGraphics;
using UIKit;
using WebKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController
{
    /// <summary>
    /// 右侧 TaskBar
    /// </summary>
    public UIView TaskBarView = new();

    public State<bool> TaskBarFocusState = new(false);

    public DWebView.DWebView TaskBarWebView { get; set; }

    public record TaskBarState(bool focus, string appId);
    public Listener<TaskBarState> OnTaskbarListener = Listener<TaskBarState>.CollectEmitListener();

    public record TaskBarRect(int width, int height);
    public TaskBarRect TaskBarResize(int width, int height)
    {
        var display = UIScreen.MainScreen.Bounds;
        var uGap = width / 5;
        var x = display.Width - width - uGap;
        var y = (display.Height - height) / 2;

        TaskBarView.Frame = new CGRect(Math.Round(x), Math.Round(y), width, height);
        ResizeTaskbarFloatView(width, height);

        return new TaskBarRect(width, height);
    }

    public async Task CreateTaskBarWebView()
    {
        var taskbarInternalUrl = GetTaskbarUrl().Uri.ToString();
        TaskBarWebView = new DWebView.DWebView(
                    localeMM: DeskNMM,
                    options: new DWebView.DWebView.Options(taskbarInternalUrl) { AllowDwebScheme = false })
        {
            Opaque = false,
            BackgroundColor = UIColor.Clear
        };

        TaskBarWebView.ScrollView.BackgroundColor = UIColor.Clear;
        _ = TaskBarWebView.LoadURL(taskbarInternalUrl).NoThrow();

        TaskBarFocusState.OnChange += async (v, ov, _) =>
        {
            if (v == ov)
            {
                return;
            }

            await OnTaskbarListener.Emit(new TaskBarState(TaskBarFocusState.Get(), TaskBarAppList.FirstOrDefault()?.Mmid));

            if (v)
            {
                foreach (var view in View.Subviews)
                {
                    if (view.Tag == 32767)
                    {
                        view.RemoveFromSuperview();
                    }
                }

                CreateTaskBarView();
            }
            else
            {
                foreach (var view in View.Subviews)
                {
                    if (view.Tag == 32768)
                    {
                        view.RemoveFromSuperview();
                    }
                }

                ShowFloatView();
            }
        };
    }

    private void OnTaskBarTap(UITapGestureRecognizer tap)
    {
        if (tap.State == UIGestureRecognizerState.Ended)
        {
            TaskBarFocusState.Set(false);
        }
    }

    public void CreateTaskBarView()
    {
        var bounds = UIScreen.MainScreen.Bounds;

        var taskbarParentView = new UIView
        {
            Frame = bounds,
            BackgroundColor = UIColor.Clear,
            Tag = 32768
        };

        View.AddSubview(taskbarParentView);

        var taskbarBackView = new UIView
        {
            Frame = bounds,
            BackgroundColor = UIColor.DarkText,
            Alpha = 0.5f
        };
        taskbarParentView.AddSubview(taskbarBackView);

        var tapGesture = new UITapGestureRecognizer(OnTaskBarTap);
        taskbarBackView.AddGestureRecognizer(tapGesture);

        {
            TaskBarView.ClipsToBounds = true;
            TaskBarView.Layer.CornerRadius = 20f;
            TaskBarView.Layer.ZPosition = nfloat.MaxValue;

            taskbarParentView.AddSubview(TaskBarView);
            /// 背景层
            {
                var effect = UIBlurEffect.FromStyle(UIBlurEffectStyle.Dark);
                var backgroundView = new UIVisualEffectView(effect);
                backgroundView.ContentView.BackgroundColor = UIColor.FromWhiteAlpha(0.7f, 0.3f);
                TaskBarView.AddSubview(backgroundView);

                /// 布局伸缩到父级
                backgroundView.AutoResize("backgroundView", TaskBarView);
            }

            TaskBarView.AddSubview(TaskBarWebView);
            TaskBarWebView.AutoResize("TaskBarWebView", TaskBarView);
        }
    }

    public List<TaskAppsStore.TaskApps> TaskBarAppList = TaskAppsStore.Instance.All();
    public async Task<List<DeskNMM.DesktopAppMetadata>> GetTaskbarAppList(int limit)
    {
        Dictionary<Mmid, DeskNMM.DesktopAppMetadata> apps = new();

        foreach (var app in TaskBarAppList)
        {
            if (apps.Count >= limit)
            {
                break;
            }
            if (app.Mmid == DeskNMM.Mmid || apps.ContainsKey(app.Mmid))
            {
                continue;
            }

            var metadata = await DeskNMM.BootstrapContext.Dns.Query(app.Mmid);
            if (metadata is not null)
            {
                var deskApp = DeskNMM.DesktopAppMetadata.FromMicroModuleManifest(metadata);
                deskApp.Running = DeskNMM.RunningApps.ContainsKey(metadata.Mmid);

                apps.Add(app.Mmid, deskApp);
            }
        }

        return apps.Values.ToList();
    }

    public URL GetTaskbarUrl() => new(TaskbarServer.StartResult.urlInfo.BuildInternalUrl()
        .Path("/taskbar.html"));

    public URL GetTaskbarApiUrl() =>
        GetTaskbarUrl().SearchParamsSet("api-base", TaskbarServer.StartResult.urlInfo.BuildPublicUrl().ToString());
}

